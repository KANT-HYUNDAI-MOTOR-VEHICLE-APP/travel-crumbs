package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.model.Trip
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            tripRepository.activeTrip
                .map { it?.tripUuid }
                .distinctUntilChanged()
                .collectLatest { tripUuid ->
                    if (tripUuid == null) return@collectLatest
                    // 1) 여행이 바뀌면(시작 · 앱 재실행) 먼저 앱 상태를 서버에 맞춤
                    val server = tripRepository.activeTrip.value?.toRecordingStatus()
                        ?: return@collectLatest
                    if (recordingRepository.progress.first().status != server) {
                        recordingRepository.togglePause()
                    }
                    // 2) 맞춘 뒤부터 앱 상태가 바뀌면 서버를 맞춤
                    //    (순서를 고정해 재실행 때 서버에 「다시 기록 → 일시정지」가 잘못 가지 않게 함)
                    recordingRepository.progress
                        .map { it.status }
                        .distinctUntilChanged()
                        .collect { local ->
                            val trip = tripRepository.activeTrip.value ?: return@collect
                            if (local == trip.toRecordingStatus()) return@collect
                            when (local) {
                                RecordingStatus.PAUSED -> tripRepository.pause(trip.tripUuid)
                                RecordingStatus.RECORDING -> tripRepository.resume(trip.tripUuid)
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
