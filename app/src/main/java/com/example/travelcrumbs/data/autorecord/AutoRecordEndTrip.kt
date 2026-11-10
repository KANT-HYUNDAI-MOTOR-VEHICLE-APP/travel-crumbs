package com.example.travelcrumbs.data.autorecord

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.EndTripUseCase
import javax.inject.Inject

/**
 * 「여행 종료」 한 곳(#66 스펙 3.8) — 기록 대기 확정 → 여행 종료 순서(N3 결정). 정차 홈 · 주행 화면이 같은 이 동작을 부름.
 * 기록 상태 저장소([RecordingRepository])는 대기 · 확정을 모름.
 */
class AutoRecordEndTrip @Inject constructor(
    private val recorder: AutoPlaceRecorder,
    private val recording: RecordingRepository,
) : EndTripUseCase {
    override suspend fun invoke(): Long? = recorder.withEndTrip { recording.endTrip() }
}
