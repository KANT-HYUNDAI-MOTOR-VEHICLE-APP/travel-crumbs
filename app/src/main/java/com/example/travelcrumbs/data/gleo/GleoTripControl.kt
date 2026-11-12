package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.GleoStatus
import com.example.travelcrumbs.domain.model.TripStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * CONTROL_CURRENT_TRIP(#69 스펙 3.2) — 화면의 「기록 일시정지 · 재개 · 여행 종료」와 같은 저장소.
 * togglePause() 는 토글이라 **지금 상태를 읽고 맞을 때만** 부름(같은 말을 두 번 해도 반대로 뒤집히지 않음).
 * END 는 저장소를 바꾸지 않고 기존 확인 팝업만 요청(U2).
 */
class GleoTripControl @Inject constructor(
    private val recording: RecordingRepository,
) {
    suspend fun control(operation: String): GleoOutcome {
        val progress = recording.progress.first()
        if (progress.tripNo == null || !progress.status.inProgress) {
            return fail(GleoCommandPhase.NO_TRIP, AppConfig.GLEO_NO_TRIP_MESSAGE)
        }
        return when (operation) {
            AppConfig.GLEO_OP_PAUSE -> if (progress.status == TripStatus.PAUSED) {
                ok(GleoCommandPhase.ALREADY_PAUSED, AppConfig.GLEO_ALREADY_PAUSED_MESSAGE)
            } else {
                toggleTo(TripStatus.PAUSED, GleoCommandPhase.TRIP_PAUSED, AppConfig.GLEO_PAUSED_MESSAGE)
            }
            AppConfig.GLEO_OP_RESUME -> if (progress.status == TripStatus.RECORDING) {
                ok(GleoCommandPhase.ALREADY_RUNNING, AppConfig.GLEO_ALREADY_RUNNING_MESSAGE)
            } else {
                toggleTo(TripStatus.RECORDING, GleoCommandPhase.TRIP_RESUMED, AppConfig.GLEO_RESUMED_MESSAGE)
            }
            AppConfig.GLEO_OP_END -> GleoOutcome(
                phase = GleoCommandPhase.END_CONFIRM,
                status = GleoStatus.SUCCESS,
                message = AppConfig.GLEO_END_CONFIRM_MESSAGE,
                screen = GleoScreenRequest.ConfirmEndTrip,
            )
            else -> fail(GleoCommandPhase.INVALID_REQUEST, AppConfig.GLEO_INPUT_FAIL_MESSAGE)
        }
    }

    private suspend fun toggleTo(target: TripStatus, phase: GleoCommandPhase, message: String): GleoOutcome {
        recording.togglePause()
        // 바뀐 상태를 다시 읽어 실제로 그 상태가 됐을 때만 성공이라고 말함.
        val after = recording.progress.first().status
        return if (after == target) ok(phase, message) else fail(GleoCommandPhase.REQUEST_FAILED, AppConfig.GLEO_CONTROL_FAIL_MESSAGE)
    }

    private fun ok(phase: GleoCommandPhase, message: String) = GleoOutcome(phase, GleoStatus.SUCCESS, message)
    private fun fail(phase: GleoCommandPhase, message: String) = GleoOutcome(phase, GleoStatus.FAIL, message)

}
