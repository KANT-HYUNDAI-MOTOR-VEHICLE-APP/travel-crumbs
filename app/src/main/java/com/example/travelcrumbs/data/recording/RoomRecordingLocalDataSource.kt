package com.example.travelcrumbs.data.recording

import androidx.room.withTransaction
import com.example.travelcrumbs.data.local.AppDatabase
import com.example.travelcrumbs.data.local.TravelDao
import com.example.travelcrumbs.data.local.TripCompanionDtlEntity
import com.example.travelcrumbs.data.local.TripMstEntity
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 지금 여행 = 지금 사용자의 가장 최근 trip_mst 행(#51 U1) — 옛 DataStore 요약 대신. 장소 수는 trip_place_dtl 개수.
 * 처음 쓸 때 한 번: 옛 DataStore 에 진행 중(RECORDING · PAUSED) 여행이 있으면 trip_mst 로 옮기고 DataStore 값은 지움(끝난 여행은 버림 · 지시서 추천).
 */
@Singleton
class RoomRecordingLocalDataSource @Inject constructor(
    private val database: AppDatabase,
    private val dao: TravelDao,
    private val users: CurrentUserRepository,
    private val legacy: DataStoreRecordingLocalDataSource,
    private val timeSource: TimeSource,
) : RecordingLocalDataSource {

    private val migrateLock = Mutex()

    @Volatile
    private var migrated = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override val trip: Flow<CurrentTrip?> = flow {
        val userNo = prepare()
        emitAll(
            dao.latestTrip(userNo).flatMapLatest { row ->
                if (row == null) flowOf(null) else dao.placeCount(row.tripNo).map { row.toCurrent(it) }
            },
        )
    }

    override suspend fun update(transform: (CurrentTrip?) -> CurrentTrip?) {
        val userNo = prepare()
        database.withTransaction {
            val row = dao.latestTripOnce(userNo)
            val next = transform(row?.toCurrent(placeCount = 0)) ?: return@withTransaction
            write(userNo, row, next)
        }
    }

    /** 사용자 번호 · 옛 값 옮기기를 한 번 — 사용자 번호를 돌려줌. */
    private suspend fun prepare(): Long {
        val userNo = users.currentUserNo()
        if (!migrated) {
            migrateLock.withLock {
                if (!migrated) {
                    // #6 판 키도 새 키로 옮겨 둔 뒤 읽음(DataStore 판의 update 가 옮김 · 값은 그대로)
                    legacy.update { it }
                    val old = legacy.trip.first()
                    if (old != null && old.status.inProgress && dao.latestTripOnce(userNo) == null) {
                        write(userNo, row = null, next = old.copy(tripNo = null))
                    }
                    if (old != null) legacy.clear()
                    migrated = true
                }
            }
        }
        return userNo
    }

    /** [next] 의 번호가 없으면 새 행(+ 동행자) · 있으면 그 행을 고침. */
    private suspend fun write(userNo: Long, row: TripMstEntity?, next: CurrentTrip) {
        val now = timeSource.nowMillis()
        val target = row?.takeIf { it.tripNo == next.tripNo }
        if (target == null) {
            val tripNo = dao.insertTrip(
                TripMstEntity(
                    userNo = userNo,
                    tripNm = next.tripName,
                    tripStatusCd = next.status.name,
                    visibilityCd = Visibility.PRIVATE.name,
                    startAt = next.startAtMillis,
                    endAt = next.endAtMillis,
                    duration = next.durationSeconds(),
                    destPlaceNm = next.destPlaceNm,
                    accumulatedMillis = next.accumulatedMillis,
                    segmentStartMillis = next.segmentStartMillis,
                    createdAt = now,
                    createdUserNo = userNo,
                    updatedAt = now,
                    updatedUserNo = userNo,
                ),
            )
            if (next.companions.isNotEmpty()) {
                dao.insertCompanions(
                    next.companions.sortedBy { it.ordinal }.map {
                        TripCompanionDtlEntity(
                            tripNo = tripNo,
                            companionTypeCd = it.name,
                            createdAt = now,
                            createdUserNo = userNo,
                            updatedAt = now,
                            updatedUserNo = userNo,
                        )
                    },
                )
            }
        } else {
            dao.updateTrip(
                target.copy(
                    tripNm = next.tripName,
                    tripStatusCd = next.status.name,
                    startAt = next.startAtMillis,
                    endAt = next.endAtMillis,
                    duration = next.durationSeconds(),
                    destPlaceNm = next.destPlaceNm,
                    accumulatedMillis = next.accumulatedMillis,
                    segmentStartMillis = next.segmentStartMillis,
                    updatedAt = now,
                    updatedUserNo = userNo,
                ),
            )
        }
    }

    private fun TripMstEntity.toCurrent(placeCount: Int): CurrentTrip = CurrentTrip(
        tripName = tripNm,
        status = TripStatus.entries.firstOrNull { it.name == tripStatusCd } ?: TripStatus.INTERRUPTED,
        startAtMillis = startAt,
        endAtMillis = endAt,
        placeCount = placeCount,
        accumulatedMillis = accumulatedMillis,
        segmentStartMillis = segmentStartMillis,
        tripNo = tripNo,
        destPlaceNm = destPlaceNm,
    )

    /** 기록한 시간(초 · 표준 duration) — 끝난 여행만. */
    private fun CurrentTrip.durationSeconds(): Long? =
        if (status.inProgress) null else accumulatedMillis / MILLIS_PER_SECOND

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
