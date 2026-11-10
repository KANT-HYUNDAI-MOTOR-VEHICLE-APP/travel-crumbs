package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.domain.model.GleoStatus
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
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
 * 응답 기한에 준비만 중단하고 이미 시작한 기록 작업은 실제 종료까지 기다림.
 */
class GleoCommandProcessor(
    private val scope: CoroutineScope,
    private val readiness: GleoRecordingReadiness,
    private val recordPlace: RecordPlaceUseCase,
    private val sendCallback: (GleoCallback) -> Unit,
    private val recordEnabled: Boolean,
    private val newRequestId: () -> UUID = UUID::randomUUID,
) {
    private val recording = AtomicBoolean(false)
    private val notices = AtomicLong(0L)
    private val _state = MutableStateFlow(GleoCommandState())
    val state: StateFlow<GleoCommandState> = _state.asStateFlow()

    fun handle(request: GleoIntentRequest) {
        when (request.functionName) {
            AppConfig.GLEO_FUNCTION_OPEN -> handleOpen(request)
            AppConfig.GLEO_FUNCTION_RECORD -> if (recordEnabled) handleRecord(request)
        }
    }

    /** 오래된 표시 해제가 새 결과를 지우지 않도록 화면의 표시 ID 대조. */
    fun clearNotice(noticeId: Long) {
        _state.update { current ->
            if (current.noticeId == noticeId && !current.phase.inProgress) {
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
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val responded = AtomicBoolean(false)
            val preparation = async(start = CoroutineStart.LAZY) {
                runCatching {
                    readiness.check { restoring ->
                        val phase = if (restoring) GleoCommandPhase.LOADING_TRIP else GleoCommandPhase.PREPARING
                        _state.update { current ->
                            if (current.noticeId == noticeId && !responded.get()) current.copy(phase = phase) else current
                        }
                    }
                }
            }
            // 준비 조회와 업무 실행 전체를 하나의 응답 예산으로 취급.
            val deadline = launch(start = CoroutineStart.UNDISPATCHED) {
                delay(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
                if (responded.compareAndSet(false, true)) {
                    _state.value = GleoCommandState(noticeId, GleoCommandPhase.UNCONFIRMED)
                    respondRecord(GleoCommandPhase.UNCONFIRMED, timedOut = true)
                    // 읽기 전용 준비 대기만 종료. 이미 시작한 저장은 취소하지 않음.
                    preparation.cancel()
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
                if (!request.readable || GleoActionParser.parse(request.functionName, request.actionParam).isFailure) {
                    finish(GleoCommandPhase.INVALID_REQUEST)
                    return@launch
                }
                _state.value = GleoCommandState(noticeId, GleoCommandPhase.PREPARING)
                when (preparation.await().getOrThrow()) {
                    GleoReadiness.NOT_READY -> finish(GleoCommandPhase.NOT_READY)
                    GleoReadiness.CONSENT_REQUIRED -> finish(GleoCommandPhase.CONSENT_REQUIRED)
                    GleoReadiness.UNCONFIRMED -> finish(GleoCommandPhase.UNCONFIRMED)
                    GleoReadiness.READY -> {
                        if (!responded.get()) {
                            val requestId = runCatching(newRequestId).getOrNull()
                            if (requestId == null) {
                                finish(GleoCommandPhase.FAILED)
                                return@launch
                            }
                            _state.value = GleoCommandState(noticeId, GleoCommandPhase.PROCESSING)
                            finish(
                                when (recordPlace(requestId)) {
                                    ActionResult.DONE -> GleoCommandPhase.SAVED
                                    ActionResult.NOT_READY -> GleoCommandPhase.NOT_READY
                                    ActionResult.FAILED -> GleoCommandPhase.FAILED
                                    ActionResult.UNCONFIRMED -> GleoCommandPhase.UNCONFIRMED
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
                preparation.cancel()
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
            GleoCommandPhase.FAILED -> AppConfig.GLEO_RECORD_FAIL_MESSAGE
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
}
