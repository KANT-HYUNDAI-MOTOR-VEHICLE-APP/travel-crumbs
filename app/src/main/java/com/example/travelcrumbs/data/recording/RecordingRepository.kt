package com.example.travelcrumbs.data.recording

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.TripStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

/**
 * 지금 여행 상태 · 경과의 단일 출처(#6 D1 · 스펙 app-shell-v5 3-4) — 홈 · 주행 화면이 같은 값을 봄.
 * route(위치 수집)는 뒤에 [progress] 를 구독.
 */
interface RecordingRepository {
    /** 상태 + 경과(분) + 여행명 · 장소 수 — 기록 중이면 분이 바뀔 때마다 새 값. */
    val progress: Flow<RecordingProgress>

    /** 「기록 일시정지」 · 「기록 재개」 — RECORDING ↔ PAUSED · 그 밖 상태는 그대로(D30 · V64 · 이름은 gleo-ai/main 과 같게 · N16). */
    suspend fun togglePause()

    /** 「여행 종료」 팝업의 「종료」 — 진행 중 여행(RECORDING · PAUSED)이면 COMPLETED · 이미 끝났으면 그대로(V21). */
    suspend fun endTrip()

    /** 임시 「새 여행 시작」(D26) — 진행 중 여행이 아닐 때만 새 여행 RECORDING · RECORDING · PAUSED 면 아무 일도 안 함(N19). */
    suspend fun startNewTrip()
}

@Singleton
class DefaultRecordingRepository @Inject constructor(
    private val local: RecordingLocalDataSource,
    private val timeSource: TimeSource,
) : RecordingRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val progress: Flow<RecordingProgress> = local.trip
        .onEach { stored ->
            // 저장된 여행이 없으면(설치 뒤 첫 실행 · #6 판 값만 있음) 옮기거나 기록 중 · 0분으로 시작(#6 D2 · N10)
            if (stored == null) {
                local.update { current -> current ?: CurrentTrip.start(timeSource.nowMillis()) }
            }
        }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { trip -> trip.ticks() }
        .distinctUntilChanged()

    override suspend fun togglePause() {
        local.update { current ->
            val now = timeSource.nowMillis()
            (current ?: CurrentTrip.start(now)).pauseToggled(now)
        }
    }

    override suspend fun endTrip() {
        local.update { current ->
            val now = timeSource.nowMillis()
            (current ?: CurrentTrip.start(now)).ended(now)
        }
    }

    override suspend fun startNewTrip() {
        local.update { current ->
            if (current != null && current.status.inProgress) current else CurrentTrip.start(timeSource.nowMillis())
        }
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
    )

    private companion object {
        const val UNIT = AppConfig.ELAPSED_UNIT_MILLIS
    }
}
