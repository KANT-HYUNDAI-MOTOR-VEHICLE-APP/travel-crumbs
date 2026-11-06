package com.example.travelcrumbs.data.recording

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.RecordingSession
import com.example.travelcrumbs.domain.model.RecordingStatus
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
 * 기록 상태 · 경과의 단일 출처(스펙 app-shell D1) — 홈 · 주행 화면이 같은 값을 봄.
 * route(위치 수집)는 뒤에 [progress] 를 구독.
 */
interface RecordingRepository {
    /** 상태 + 경과(분) — 기록 중이면 분이 바뀔 때마다 새 값. */
    val progress: Flow<RecordingProgress>

    /** 기록 중 ↔ 일시정지. */
    suspend fun togglePause()
}

@Singleton
class DefaultRecordingRepository @Inject constructor(
    private val local: RecordingLocalDataSource,
    private val timeSource: TimeSource,
) : RecordingRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val progress: Flow<RecordingProgress> = local.session
        .onEach { stored ->
            // 설치 뒤 첫 실행 — 저장된 기록 상태가 없으면 기록 중 · 0분으로 시작(스펙 D2)
            if (stored == null) {
                local.update { current -> current ?: RecordingSession.start(timeSource.nowMillis()) }
            }
        }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { session -> session.ticks() }
        .distinctUntilChanged()

    override suspend fun togglePause() {
        local.update { current ->
            val now = timeSource.nowMillis()
            (current ?: RecordingSession.start(now)).toggled(now)
        }
    }

    private fun RecordingSession.ticks(): Flow<RecordingProgress> {
        if (status == RecordingStatus.PAUSED) {
            return flowOf(RecordingProgress(status, elapsedMillis(timeSource.nowMillis()) / UNIT))
        }
        return flow {
            while (true) {
                val elapsed = elapsedMillis(timeSource.nowMillis())
                emit(RecordingProgress(status, elapsed / UNIT))
                // 다음 분 경계까지 잠듦 — 초 단위로 깨지 않음(주행 중 CPU)
                delay(UNIT - elapsed % UNIT)
            }
        }
    }

    private companion object {
        const val UNIT = AppConfig.ELAPSED_UNIT_MILLIS
    }
}
