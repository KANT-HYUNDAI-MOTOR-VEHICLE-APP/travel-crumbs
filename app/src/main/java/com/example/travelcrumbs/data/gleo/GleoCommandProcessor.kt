package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.GleoAction
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
 * 한 번에 한 Gleo 명령(#69 3.1) — 처리 중 다른 함수가 오면 즉시 BUSY 실패. 같은 RECORD 가 겹치면 #25 대로 합침(응답 없음).
 * [writeEnabled] = 기록 · 여행 제어를 받을지(release 는 R1 확인 전까지 거짓 · U1). 정보 조회는 늘 받음.
 */
class GleoCommandProcessor(
    private val scope: CoroutineScope,
    private val readiness: GleoRecordingReadiness,
    private val recordPlace: RecordPlaceUseCase,
    private val sendCallback: (GleoCallback) -> Unit,
    private val writeEnabled: Boolean,
    private val tripControl: suspend (operation: String) -> GleoOutcome,
    private val travelInfo: suspend (infoType: String, region: String?) -> GleoOutcome,
    private val newRequestId: () -> UUID = UUID::randomUUID,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val recording = AtomicBoolean(false)
    @Volatile private var busyFunction: String? = null
    private val notices = AtomicLong(0L)
    private val _state = MutableStateFlow(GleoCommandState())
    val state: StateFlow<GleoCommandState> = _state.asStateFlow()

    fun handle(request: GleoIntentRequest) {
        when (request.functionName) {
            AppConfig.GLEO_FUNCTION_RECORD -> if (writeEnabled) handleRecord(request)
            AppConfig.GLEO_FUNCTION_CONTROL -> if (writeEnabled) {
                handleQuick(request) { action -> tripControl(action.parameters.getValue(AppConfig.GLEO_PARAM_OPERATION)) }
            }
            AppConfig.GLEO_FUNCTION_INFO -> handleQuick(request) { action ->
                travelInfo(action.parameters.getValue(AppConfig.GLEO_PARAM_INFO_TYPE), action.parameters[AppConfig.GLEO_PARAM_REGION])
            }
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

    /**
     * 여행 제어 · 정보 조회(#69) — 준비 확인 없이 바로 저장소를 부르는 짧은 명령.
     * 7초 안에 끝나지 않으면 그때 실패로 한 번만 응답(늦은 결과로 두 번째 콜백 없음). 화면 요청은 기한 안 결과에만 붙음.
     */
    private fun handleQuick(request: GleoIntentRequest, run: suspend (GleoAction) -> GleoOutcome) {
        if (!recording.compareAndSet(false, true)) {
            respondBusy(request.functionName)
            return
        }
        busyFunction = request.functionName
        val noticeId = notices.incrementAndGet()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val responded = AtomicBoolean(false)
            fun finish(outcome: GleoOutcome) {
                if (!responded.compareAndSet(false, true)) return
                _state.value = GleoCommandState(noticeId, outcome.phase, outcome.detail, outcome.screen, clock())
                respond(GleoCallback(request.functionName, outcome.status, outcome.message, outcome.data))
            }
            val deadline = launch(start = CoroutineStart.UNDISPATCHED) {
                delay(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
                finish(GleoOutcome(GleoCommandPhase.REQUEST_FAILED, GleoStatus.FAIL, AppConfig.GLEO_TIMEOUT_MESSAGE))
            }
            try {
                val action = GleoActionParser.parse(request.functionName, request.actionParam).getOrNull()
                if (!request.readable || action == null) {
                    finish(GleoOutcome(GleoCommandPhase.INVALID_REQUEST, GleoStatus.FAIL, AppConfig.GLEO_INPUT_FAIL_MESSAGE))
                    return@launch
                }
                finish(run(action))
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) finish(failure(request.functionName)) else throw cancelled
            } catch (_: Exception) {
                finish(failure(request.functionName))
            } finally {
                deadline.cancel()
                busyFunction = null
                recording.set(false)
            }
        }
    }

    private fun failure(functionName: String) = GleoOutcome(
        GleoCommandPhase.REQUEST_FAILED,
        GleoStatus.FAIL,
        if (functionName == AppConfig.GLEO_FUNCTION_INFO) AppConfig.GLEO_INFO_FAIL_MESSAGE else AppConfig.GLEO_CONTROL_FAIL_MESSAGE,
    )

    /** 다른 명령을 처리 중 — 상태 · 화면은 그대로 두고 이번 명령에만 실패로 답함. */
    private fun respondBusy(functionName: String) {
        respond(GleoCallback(functionName, GleoStatus.FAIL, AppConfig.GLEO_BUSY_MESSAGE))
    }

    private fun handleRecord(request: GleoIntentRequest) {
        // 요청 ID가 없는 같은 Function의 중복을 합침(#25). 다른 함수가 처리 중이면 BUSY 로 답함(#69).
        if (!recording.compareAndSet(false, true)) {
            if (busyFunction != AppConfig.GLEO_FUNCTION_RECORD) respondBusy(request.functionName)
            return
        }
        busyFunction = request.functionName
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
                busyFunction = null
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
