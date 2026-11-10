package com.example.travelcrumbs.data.recording

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.NewTrip
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.TripStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * 지금 여행 상태 · 경과의 단일 출처(#6 D1 · 스펙 app-shell-v5 3-4) — 홈 · 주행 화면이 같은 값을 봄.
 * route(위치 수집)는 뒤에 [progress] 를 구독.
 */
interface RecordingRepository {
    /** 상태 + 경과(분) + 여행명 · 장소 수 — 기록 중이면 분이 바뀔 때마다 새 값. */
    val progress: Flow<RecordingProgress>

    /** 「기록 일시정지」 · 「기록 재개」 — RECORDING ↔ PAUSED · 그 밖 상태는 그대로(D30 · V64 · 이름은 gleo-ai/main 과 같게 · N16). */
    suspend fun togglePause()

    /** 「여행 종료」 팝업의 「종료」 — 진행 중 여행(RECORDING · PAUSED)이면 COMPLETED · 이미 끝났으면 그대로(V21).
     * 돌려주는 값 = 이번에 정한 종료 시각(이미 끝난 여행이면 null) — 종료 시각의 출처는 이 한 곳(별점 #41 이 받아 씀).
     */
    suspend fun endTrip(): Long?

    /** 이름 없는 새 여행 시작 — [startTrip] 과 같은 규칙(시험 · 옛 호출 자리). */
    suspend fun startNewTrip() {
        startTrip(NewTrip(tripNm = null, companions = emptySet(), destPlaceNm = null))
    }

    /**
     * A-01 「기록 시작」(#51 U3) — 진행 중 여행이 아닐 때만 새 여행 RECORDING · PRIVATE(이름 · 동행자 · 목적지).
     * 진행 중(RECORDING · PAUSED)이면 아무 일도 안 하고 false(N19).
     */
    suspend fun startTrip(newTrip: NewTrip): Boolean
}

@Singleton
class DefaultRecordingRepository @Inject constructor(
    private val local: RecordingLocalDataSource,
    private val timeSource: TimeSource,
) : RecordingRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    // v6(#51): 여행이 없으면 만들지 않음 — 「여행 시작」(A-01)으로만 생김 · 없음은 [RecordingProgress.NONE]
    override val progress: Flow<RecordingProgress> = local.trip
        .distinctUntilChanged()
        .flatMapLatest { trip -> trip?.ticks() ?: flowOf(RecordingProgress.NONE) }
        .distinctUntilChanged()

    override suspend fun togglePause() {
        local.update { current -> current?.pauseToggled(timeSource.nowMillis()) }
    }

    override suspend fun endTrip(): Long? {
        var endedAt: Long? = null
        local.update { current ->
            val before = current ?: return@update null
            // 이번에 끝냈나 = 종료 전 상태가 진행 중(RECORDING · PAUSED)이었나 — 그때만 종료 시각을 돌려줌
            before.ended(timeSource.nowMillis()).also { after -> endedAt = if (before.status.inProgress) after.endAtMillis else null }
        }
        return endedAt
    }

    override suspend fun startTrip(newTrip: NewTrip): Boolean {
        var started = false
        local.update { current ->
            if (current != null && current.status.inProgress) {
                null
            } else {
                started = true
                CurrentTrip.start(timeSource.nowMillis(), newTrip)
            }
        }
        return started
    }

    private fun CurrentTrip.ticks(): Flow<RecordingProgress> {
        if (status != TripStatus.RECORDING) {
            return flowOf(progressAt(timeSource.nowMillis()))
        }
        return flow {
            while (true) {
                val now = timeSource.nowMillis()
                emit(progressAt(now))
                // 다음 분 경계까지 잠듦(RECORDING 만 · PAUSED · 종료 상태는 값 하나로 멈춤) — 초 단위로 깨지 않음(주행 중 CPU)
                delay(UNIT - elapsedMillis(now) % UNIT)
            }
        }
    }

    private fun CurrentTrip.progressAt(nowMillis: Long) = RecordingProgress(
        status = status,
        elapsedMinutes = elapsedMillis(nowMillis) / UNIT,
        tripName = tripName,
        placeCount = placeCount,
        tripNo = tripNo,
        destPlaceNm = destPlaceNm,
    )

    private companion object {
        const val UNIT = AppConfig.ELAPSED_UNIT_MILLIS
    }
}
