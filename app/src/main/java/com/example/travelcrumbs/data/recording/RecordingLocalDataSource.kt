package com.example.travelcrumbs.data.recording

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.data.local.AppPreferencesKeys.Legacy
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.TripStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 지금 여행 요약을 기기 안에 남김 — 앱 종료 · 재부팅 뒤 복원(#6 D3 · 스펙 app-shell-v5 4-1).
 * 바인딩은 Room 판([RoomRecordingLocalDataSource] · trip_mst · #51 U1). DataStore 판은 옛 기기 값을 옮길 때만 읽음.
 */
interface RecordingLocalDataSource {
    /** 저장된 지금 여행 요약 — 없으면 null(#6 판 값만 있어도 null · [update] 가 옮김). */
    val trip: Flow<CurrentTrip?>

    /**
     * 읽고 바꾸기를 한 번에(원자적) — 두 화면이 거의 동시에 눌러도 어긋나지 않게.
     * [transform] 이 null 을 돌려주면 아무것도 바꾸지 않음(여행이 없는데 일시정지 · 종료 등).
     * DataStore 판: 새 키가 비어 있고 #6 판 값이 있으면 그 값을 옮겨 [transform] 에 넘기고 옛 키를 지움(N10).
     */
    suspend fun update(transform: (CurrentTrip?) -> CurrentTrip?)
}

@Singleton
class DataStoreRecordingLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeSource: TimeSource,
) : RecordingLocalDataSource {

    override val trip: Flow<CurrentTrip?> = dataStore.data.map { it.toTrip() }

    override suspend fun update(transform: (CurrentTrip?) -> CurrentTrip?) {
        dataStore.edit { prefs ->
            val current = prefs.toTrip() ?: prefs.legacyTrip(timeSource.nowMillis())
            transform(current)?.let { prefs.write(it) }
            prefs.remove(Legacy.RECORDING_STATUS)
            prefs.remove(Legacy.RECORDING_ACCUMULATED_MILLIS)
            prefs.remove(Legacy.RECORDING_SEGMENT_START_MILLIS)
        }
    }

    /** 옮긴 뒤 지움 — 지금 여행 요약 키 전부(#51 U1 · Room 으로 옮긴 뒤 한 번). */
    suspend fun clear() {
        dataStore.edit { prefs ->
            listOf(
                AppPreferencesKeys.TRIP_STATUS,
                AppPreferencesKeys.TRIP_NAME,
                AppPreferencesKeys.TRIP_START_AT_MILLIS,
                AppPreferencesKeys.TRIP_END_AT_MILLIS,
                AppPreferencesKeys.TRIP_PLACE_COUNT,
                AppPreferencesKeys.TRIP_ACCUMULATED_MILLIS,
                AppPreferencesKeys.TRIP_SEGMENT_START_MILLIS,
                Legacy.RECORDING_STATUS,
                Legacy.RECORDING_ACCUMULATED_MILLIS,
                Legacy.RECORDING_SEGMENT_START_MILLIS,
            ).forEach { prefs.remove(it) }
        }
    }

    private fun MutablePreferences.write(trip: CurrentTrip) {
        this[AppPreferencesKeys.TRIP_STATUS] = trip.status.name
        this[AppPreferencesKeys.TRIP_START_AT_MILLIS] = trip.startAtMillis
        this[AppPreferencesKeys.TRIP_PLACE_COUNT] = trip.placeCount
        this[AppPreferencesKeys.TRIP_ACCUMULATED_MILLIS] = trip.accumulatedMillis
        this[AppPreferencesKeys.TRIP_SEGMENT_START_MILLIS] = trip.segmentStartMillis
        val end = trip.endAtMillis
        if (end == null) remove(AppPreferencesKeys.TRIP_END_AT_MILLIS) else this[AppPreferencesKeys.TRIP_END_AT_MILLIS] = end
        val name = trip.tripName
        if (name == null) remove(AppPreferencesKeys.TRIP_NAME) else this[AppPreferencesKeys.TRIP_NAME] = name
    }

    private fun Preferences.toTrip(): CurrentTrip? {
        val status = this[AppPreferencesKeys.TRIP_STATUS]?.let(::tripStatusOf) ?: return null
        val startAt = this[AppPreferencesKeys.TRIP_START_AT_MILLIS] ?: 0L
        val endAt = this[AppPreferencesKeys.TRIP_END_AT_MILLIS]
        val accumulated = this[AppPreferencesKeys.TRIP_ACCUMULATED_MILLIS]
        val trip = CurrentTrip(
            tripName = this[AppPreferencesKeys.TRIP_NAME],
            status = status,
            startAtMillis = startAt,
            endAtMillis = endAt,
            placeCount = this[AppPreferencesKeys.TRIP_PLACE_COUNT] ?: 0,
            accumulatedMillis = accumulated ?: 0L,
            segmentStartMillis = this[AppPreferencesKeys.TRIP_SEGMENT_START_MILLIS] ?: startAt,
        )
        return if (accumulated == null) firstReleaseTripToCurrent(trip) else trip
    }

    private fun Preferences.legacyTrip(nowMillis: Long): CurrentTrip? {
        val status = this[Legacy.RECORDING_STATUS] ?: return null
        return legacyRecordingToTrip(
            status = status,
            accumulatedMillis = this[Legacy.RECORDING_ACCUMULATED_MILLIS] ?: 0L,
            segmentStartMillis = this[Legacy.RECORDING_SEGMENT_START_MILLIS] ?: nowMillis,
            nowMillis = nowMillis,
        )
    }
}

private fun tripStatusOf(name: String): TripStatus? = TripStatus.entries.firstOrNull { it.name == name }

/**
 * #21 첫 판(PR #23) 저장 값 — 누적 기록 시간 · 구간 시작 칸이 없음(N10 추천 (가)).
 * 첫 판엔 일시정지가 없었으므로 기록 시간 = (종료 시각 또는 지금) − 시작 시각:
 * RECORDING 은 누적 0 · 구간 시작 = 시작 시각(지금까지 이어서 늘어남) · 그 밖은 종료 − 시작으로 고정.
 */
fun firstReleaseTripToCurrent(trip: CurrentTrip): CurrentTrip = when (trip.status) {
    TripStatus.RECORDING -> trip.copy(accumulatedMillis = 0L, segmentStartMillis = trip.startAtMillis)
    else -> trip.copy(
        accumulatedMillis = ((trip.endAtMillis ?: trip.startAtMillis) - trip.startAtMillis).coerceAtLeast(0L),
        segmentStartMillis = trip.startAtMillis,
    )
}

/**
 * #6 판 기록 상태(RECORDING · PAUSED + 누적 기록 시간 + 지금 구간 시작) → 지금 여행 요약(N10 추천 (a)).
 * 상태 · 누적 · 구간 시작은 그대로(PAUSED 는 PAUSED 그대로 · 경과 = 저장된 누적). #6 판엔 여행 시작 시각이 없어
 * 기록 중이었으면 「구간 시작 − 누적」 · 일시정지였으면 「지금 − 누적」을 시작 시각으로 둠.
 * 알 수 없는 값이면 null(첫 실행처럼).
 */
fun legacyRecordingToTrip(
    status: String,
    accumulatedMillis: Long,
    segmentStartMillis: Long,
    nowMillis: Long,
): CurrentTrip? {
    val accumulated = accumulatedMillis.coerceAtLeast(0L)
    val tripStatus = tripStatusOf(status)?.takeIf { it.inProgress } ?: return null
    val startAt = if (tripStatus == TripStatus.RECORDING) segmentStartMillis - accumulated else nowMillis - accumulated
    return CurrentTrip(
        tripName = null,
        status = tripStatus,
        startAtMillis = startAt,
        endAtMillis = null,
        placeCount = 0,
        accumulatedMillis = accumulated,
        segmentStartMillis = segmentStartMillis,
    )
}
