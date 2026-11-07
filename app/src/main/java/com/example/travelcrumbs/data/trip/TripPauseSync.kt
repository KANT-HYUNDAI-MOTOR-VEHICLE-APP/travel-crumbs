package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.Trip
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
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
        // 여행이 바뀌면(시작 · 앱 재실행) 앱 상태를 서버에 맞춤
        scope.launch {
            tripRepository.activeTrip
                .map { it?.tripUuid }
                .distinctUntilChanged()
                .collect {
                    val trip = tripRepository.activeTrip.value ?: return@collect
                    val local = recordingRepository.progress.first().status
                    if (local != trip.toRecordingStatus()) recordingRepository.togglePause()
                }
        }
        // 앱 상태가 바뀌면 서버를 맞춤
        scope.launch {
            combine(
                recordingRepository.progress.map { it.status }.distinctUntilChanged(),
                tripRepository.activeTrip,
            ) { local, trip -> local to trip }
                .collect { (local, trip) ->
                    if (trip == null || local == trip.toRecordingStatus()) return@collect
                    when (local) {
                        RecordingStatus.PAUSED -> tripRepository.pause(trip.tripUuid)
                        RecordingStatus.RECORDING -> tripRepository.resume(trip.tripUuid)
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
