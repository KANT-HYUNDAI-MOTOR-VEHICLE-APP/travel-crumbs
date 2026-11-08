package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.domain.model.GleoStatus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * Intent·Activity 수명과 분리한 한 프로세스의 명령 처리기.
 * 6초는 응답 시도 목표일 뿐. 타이머가 끝나도 서버에서 실행 중일 수 있는 저장 작업을 취소·재실행하지 않음.
 */
class GleoCommandProcessor(
    private val scope: CoroutineScope,
    private val readiness: GleoRecordingReadiness,
    private val recordPlace: RecordPlaceUseCase,
    private val sendCallback: (GleoCallback) -> Unit,
) {
    private val recording = AtomicBoolean(false)
    private val notices = AtomicLong(0L)
    private val _state = MutableStateFlow(GleoCommandState())
    val state: StateFlow<GleoCommandState> = _state.asStateFlow()

    fun handle(request: GleoIntentRequest) {
        when (request.functionName) {
            AppConfig.GLEO_FUNCTION_OPEN -> handleOpen(request)
            AppConfig.GLEO_FUNCTION_RECORD -> handleRecord(request)
        }
    }

    /** 오래된 표시 해제가 새 결과를 지우지 않도록 화면의 표시 ID 대조. */
    fun clearNotice(noticeId: Long) {
        _state.update { current ->
            if (current.noticeId == noticeId && current.phase !in ACTIVE_PHASES) {
                current.copy(phase = GleoCommandPhase.IDLE)
            } else {
                current
            }
        }
    }

    private fun handleOpen(request: GleoIntentRequest) {
        val valid = request.readable && GleoActionParser.parse(request.functionName, request.actionParam).isSuccess
        respond(
            GleoCallback(
                request.functionName,
                if (valid) GleoStatus.SUCCESS else GleoStatus.FAIL,
                if (valid) AppConfig.GLEO_OPEN_SUCCESS_MESSAGE else AppConfig.GLEO_OPEN_FAIL_MESSAGE,
            ),
        )
    }

    private fun handleRecord(request: GleoIntentRequest) {
        // 요청 ID가 없는 같은 Function의 중복을 합침. 별도 동시 명령 각각의 응답은 보장하지 못함.
        if (!recording.compareAndSet(false, true)) return
        val noticeId = notices.incrementAndGet()
        if (!request.readable || GleoActionParser.parse(request.functionName, request.actionParam).isFailure) {
            _state.value = GleoCommandState(noticeId, GleoCommandPhase.INVALID_REQUEST)
            respondRecord(GleoCommandPhase.INVALID_REQUEST)
            recording.set(false)
            return
        }
        _state.value = GleoCommandState(noticeId, GleoCommandPhase.PREPARING)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val responded = AtomicBoolean(false)
            // 준비 조회와 업무 실행 전체를 하나의 응답 예산으로 취급.
            val deadline = launch(start = CoroutineStart.UNDISPATCHED) {
                delay(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
                if (responded.compareAndSet(false, true)) {
                    _state.value = GleoCommandState(noticeId, GleoCommandPhase.UNCONFIRMED)
                    respondRecord(GleoCommandPhase.UNCONFIRMED, timedOut = true)
                }
            }
            fun finish(phase: GleoCommandPhase) {
                if (responded.compareAndSet(false, true)) {
                    _state.value = GleoCommandState(noticeId, phase)
                    respondRecord(phase)
                }
                // 기한 뒤의 결과로 두 번째 콜백이나 재저장을 만들지 않음. 미확인 안내 유지.
            }
            try {
                when (readiness.check()) {
                    GleoReadiness.NOT_READY -> finish(GleoCommandPhase.NOT_READY)
                    GleoReadiness.CONSENT_REQUIRED -> finish(GleoCommandPhase.CONSENT_REQUIRED)
                    GleoReadiness.READY -> {
                        if (!responded.get()) {
                            _state.value = GleoCommandState(noticeId, GleoCommandPhase.PROCESSING)
                            finish(
                                when (recordPlace()) {
                                    ActionResult.DONE -> GleoCommandPhase.SAVED
                                    ActionResult.NOT_READY -> GleoCommandPhase.NOT_READY
                                    ActionResult.FAILED -> GleoCommandPhase.UNCONFIRMED
                                },
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) finish(GleoCommandPhase.UNCONFIRMED) else throw cancelled
            } catch (_: Exception) {
                finish(GleoCommandPhase.UNCONFIRMED)
            } finally {
                deadline.cancel()
                // 응답 여부와 독립. 기존 UseCase가 실제 반환한 뒤에만 다음 음성 기록 허용.
                recording.set(false)
            }
        }
    }

    private fun respondRecord(phase: GleoCommandPhase, timedOut: Boolean = false) {
        val message = when (phase) {
            GleoCommandPhase.SAVED -> AppConfig.GLEO_RECORD_SUCCESS_MESSAGE
            GleoCommandPhase.INVALID_REQUEST -> AppConfig.GLEO_RECORD_INPUT_FAIL_MESSAGE
            GleoCommandPhase.CONSENT_REQUIRED -> AppConfig.GLEO_RECORD_CONSENT_MESSAGE
            GleoCommandPhase.NOT_READY -> AppConfig.GLEO_RECORD_NOT_READY_MESSAGE
            else -> if (timedOut) AppConfig.GLEO_RECORD_TIMEOUT_MESSAGE else AppConfig.GLEO_RECORD_UNCONFIRMED_MESSAGE
        }
        respond(
            GleoCallback(
                AppConfig.GLEO_FUNCTION_RECORD,
                if (phase == GleoCommandPhase.SAVED) GleoStatus.SUCCESS else GleoStatus.FAIL,
                message,
            ),
        )
    }

    private fun respond(callback: GleoCallback) {
        // Broadcast 실패도 재실행·두 번째 성공 콜백의 계기가 되지 않음.
        runCatching { sendCallback(callback) }
    }

    private companion object {
        val ACTIVE_PHASES = setOf(GleoCommandPhase.PREPARING, GleoCommandPhase.PROCESSING)
    }
}
