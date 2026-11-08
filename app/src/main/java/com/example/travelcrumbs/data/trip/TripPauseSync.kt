package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.Trip
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 일시정지 맞추기(결정 로그 D27) — 앱 뼈대의 기록 상태(RecordingRepository · 주행 화면 「일시정지」 버튼)와
 * 서버 여행 상태(trip_mst RECORDING ↔ PAUSED)를 같게 유지.
 * - 버튼으로 앱 상태가 바뀌면 → 서버에 일시정지 · 다시 기록(상태 이력 1행)
 * - 앱을 다시 켜거나 여행이 바뀌면 → 앱 상태를 서버 상태에 맞춤
 * RouteRecorder 가 start(scope) 를 부름.
 */
@Singleton
class TripPauseSync @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val tripRepository: TripRepository,
) {
    fun start(scope: CoroutineScope) {
        // 여행별 초기 정렬을 끝낸 뒤에만 버튼 변경을 서버로 보냄.
        // 두 방향을 동시에 수집하면 미초기화 기본 RECORDING이 서버 PAUSED를 먼저 resume할 수 있음.
        scope.launch {
            tripRepository.activeTrip
                .map { it?.tripUuid }
                .distinctUntilChanged()
                .collectLatest { tripUuid ->
                    if (tripUuid == null) return@collectLatest
                    val local = recordingRepository.progress.first().status
                    val trip = tripRepository.activeTrip.value
                        ?.takeIf { it.tripUuid == tripUuid } ?: return@collectLatest
                    if (local != trip.toRecordingStatus()) recordingRepository.togglePause()

                    // collectLatest가 여행 전환 시 이전 여행의 초기 대기·변경 감지를 함께 취소.
                    recordingRepository.progress
                        .map { it.status }
                        .distinctUntilChanged()
                        .collect { status ->
                            val current = tripRepository.activeTrip.value
                                ?.takeIf { it.tripUuid == tripUuid } ?: return@collect
                            if (status == current.toRecordingStatus()) return@collect
                            when (status) {
                                RecordingStatus.PAUSED -> tripRepository.pause(tripUuid)
                                RecordingStatus.RECORDING -> tripRepository.resume(tripUuid)
                            }
                        }
                }
        }
    }

    private fun Trip.toRecordingStatus(): RecordingStatus =
        if (statusCode == PAUSED) RecordingStatus.PAUSED else RecordingStatus.RECORDING

    private companion object {
        const val PAUSED = "PAUSED"
    }
}
