package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 길안내 요청 상태(스펙 navigation 3.2 「길안내 요청」 · 「실패 폴백」).
 * [seq] = 상태 변경 순서 번호 — 상태가 바뀔 때마다 1씩 늘어남(대기 = 0) · 부르는 쪽이 「어느 변경 뒤의 상태인가」를 가릴 때 씀.
 * [Failed.attempt] = 그 실패를 낳은 시도 번호 — 내비에 요청을 보낼 때마다 늘고, 보내지 않은 즉시 실패(미연결)는
 * 바로 앞 실패와 같은 장소면 그 번호를 이어받음 · 부르는 쪽이 「같은 실패인가」를 가릴 때 씀.
 */
sealed interface GuideRequestState {
    val seq: Long

    data object Idle : GuideRequestState {
        override val seq: Long get() = 0L
    }

    data class Requesting(val place: Place, override val seq: Long) : GuideRequestState

    /** 요청할 때의 장소(그 뒤 고정 해제해도 · T42). */
    data class Started(val place: Place, override val seq: Long) : GuideRequestState

    data class Failed(
        val reason: RouteFailReason,
        val place: Place,
        val attempt: Long,
        override val seq: Long,
    ) : GuideRequestState
}

/**
 * 길안내 요청 입구 — 화면과 무관하게 「장소 하나로 안내 요청 → 대기 → 결과 · TIMEOUT 대조 · 늦은 시작 판정 → 핀 남기기」를 함.
 * 홈 「여기로 안내」 · 「다시 시도」가 지금 쓰는 곳이고, 음성 · 「다시 가기」 · 「이 장소만 추가」(와이어프레임 v5)가 같은 입구를 쓸 수 있음.
 *
 * 앱 하나에 하나(di 에서 Singleton) · [scope] 는 앱 수명 — 홈이 그려지지 않는 동안(주행 전용 화면 · 앱 얼림)에도
 * 내비 응답 · 한도 · 늦은 알림을 이어 받음. 주행 · 정차 판정은 부르는 쪽 몫(여기서 막지 않음).
 * 동시에 하나의 요청만 — 요청 중이면 새 [guide] 는 무시.
 * 메인 스레드에서만 부름(공개 함수 · [scope] 모두 Main) — 잠금 없는 가변 칸(바로 앞 요청 · 시도 번호)을 씀.
 */
class GuideRequestTracker(
    private val navi: NaviGuidance,
    private val recorder: GuidanceStartRecorder,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource { System.currentTimeMillis() },
) {
    private val _state = MutableStateFlow<GuideRequestState>(GuideRequestState.Idle)

    /** 마지막 길안내 요청 상태 — 결과는 다음 요청을 시작할 때까지 남음(U7). */
    val state: StateFlow<GuideRequestState> = _state.asStateFlow()

    private var attempts = 0L
    private var changes = 0L

    /** 상태를 바꾸는 유일한 길 — 변경 번호를 붙여 내보냄. */
    private fun publish(make: (seq: Long) -> GuideRequestState) {
        _state.value = make(++changes)
    }

    /**
     * 바로 앞 길안내 요청 — 결과 전(요청 중) 또는 TIMEOUT 뒤(늦은 시작 · 재개 대조를 아직 받음 · 스펙 3.2 「바로 앞 요청 X」).
     * 내비 응답([answer])은 한도가 지나도 계속 기다림 — 늦은 onRouteStarted 를 받기 위함. 새 요청을 시작하면 버림(T94).
     */
    private class GuideRequest(val place: Place, val attempt: Long, val answer: Deferred<RouteRequestResult>) {
        /** 한도가 지나 TIMEOUT 으로 보였음 — 이 뒤엔 시작됨으로만 바뀔 수 있음. */
        var timedOut = false
        val jobs = mutableListOf<Job>()

        fun stop() {
            answer.cancel()
            jobs.forEach { it.cancel() }
        }
    }

    /**
     * 바로 앞 요청 칸 — 「요청 중」 · 「TIMEOUT 뒤 기억」은 [state] 와 이 칸이 함께 나타냄(S2). 이 클래스 밖에선 보이지 않음.
     * 쓰는 곳: [guide](바꿈) · [finish](비움 · TIMEOUT 표시) · [markStarted](비움) · [forget](비움).
     * 읽는 곳: [guide](요청 중 여부) · [recheck](대조 대상) · [naviIsHeadingTo](기다린 뒤 아직 같은 요청인가).
     */
    private var lastRequest: GuideRequest? = null

    /**
     * 마지막으로 시작됨으로 바뀐 장소 + 시각 한 칸(#66 스펙 3.8 「이 앱 요청」 분류) — [markStarted] 가 씀 · [isAppRequest] 만 읽음 · 밖에 공개하지 않음.
     * [state] 의 시작됨(다음 요청까지 남음)과 달리 시각이 있어 짧은 창으로만 씀.
     */
    private var lastStartedPlace: Place? = null
    private var lastStartedAt: Long? = null

    /**
     * 읽기 전용 질의(#66 스펙 3.6 · 3.8) — [destination] 이 (a) 내부 요청 칸(결과 전 · TIMEOUT 뒤 기억 중)의 장소와 같거나
     * (b) 마지막 시작됨 장소와 같고 그때부터 [AppConfig.AUTO_RECORD_APP_REQUEST_WINDOW_MILLIS] 안인가. 상태 · 동작은 바꾸지 않음.
     * 오래 남는 [state] 의 시작됨 · 실패는 보지 않음. 메인 스레드에서만 부름(다른 공개 함수와 같음).
     */
    fun isAppRequest(destination: NaviDestination): Boolean = AutoRecordRules.isAppRequest(
        destination = destination,
        pendingRequest = lastRequest?.place,
        lastStarted = lastStartedPlace,
        lastStartedAt = lastStartedAt,
        now = timeSource.nowMillis(),
    )

    /** 지금 요청 중인가(결과 전) — TIMEOUT 뒤 기억 중은 요청 중이 아님. */
    private val isRequesting: Boolean get() = lastRequest?.timedOut == false

    /**
     * [place] 로 길안내 요청(스펙 3.2 「길안내 요청」) — 요청 중이면 무시. 주행 · 모름 거르기는 부르는 쪽 몫.
     * 그 순간 내비 연결이 없으면 요청하지 않고 실패(NOT_CONNECTED · T9).
     */
    fun guide(place: Place) {
        if (isRequesting) return
        if (!navi.ready.value) {
            // 버튼 비활성과 연결 끊김이 엇갈림 — 내비에 요청하지 않음(T9). 같은 장소의 실패를 이어받으면 시도 번호도 그대로(T99)
            val current = _state.value
            val attempt = if (current is GuideRequestState.Failed && current.place.id == place.id) {
                current.attempt
            } else {
                ++attempts
            }
            publish { GuideRequestState.Failed(RouteFailReason.NOT_CONNECTED, place, attempt, it) }
            return
        }
        // 다른(또는 같은) 장소를 새로 요청 — 바로 앞 TIMEOUT 요청은 더 대조하지 않음(T94)
        lastRequest?.stop()
        val request = GuideRequest(place, ++attempts, scope.async { navi.requestRoute(place) })
        lastRequest = request
        publish { GuideRequestState.Requesting(place, it) }
        request.jobs += scope.launch {
            var result = awaitRouteResult(navi.ready, AppConfig.ROUTE_START_TIMEOUT_MILLIS) {
                request.answer.await()
            }
            // R1 — 한도가 지남(앱이 얼려져 시작 알림을 놓친 경우 포함): 내비가 지금 그 장소로 안내 중이면 시작됨
            if (result == TIMEOUT && naviIsHeadingTo(request)) result = RouteRequestResult.Started
            finish(request, result)
        }
    }

    /** 「다시 시도」(E11) — 실패한 그 장소로 [guide] 와 같은 길. 실패가 아니면 무시(T104 · T105). 주행 · 모름 거르기는 부르는 쪽 몫. */
    fun retry() {
        val failed = _state.value as? GuideRequestState.Failed ?: return
        guide(failed.place)
    }

    /**
     * 앱이 다시 앞으로 옴(R1) — 바로 앞 요청이 아직 시작 알림을 못 받았으면(요청 중 · TIMEOUT) 내비 상태로 대조.
     * 그 장소로 안내 중이면 시작됨 · 아니면 그대로(요청 중이면 한도가 끝냄 · T95).
     */
    fun recheck() {
        val request = lastRequest ?: return
        scope.launch {
            if (naviIsHeadingTo(request)) markStarted(request)
        }
    }

    /** 요청 중인 요청의 결과(시작 · 오류 · 끊김 · 한도) — 이미 정해졌거나 TIMEOUT 뒤면 무시. */
    private fun finish(request: GuideRequest, result: RouteRequestResult) {
        if (lastRequest !== request || request.timedOut) return
        when {
            result == RouteRequestResult.Started -> markStarted(request)
            result == TIMEOUT -> {
                // TIMEOUT 으로 보이되 이 요청을 기억 — 늦은 onRouteStarted 를 받으면 시작됨(스펙 3.2 · T91)
                request.timedOut = true
                publish { GuideRequestState.Failed(RouteFailReason.TIMEOUT, request.place, request.attempt, it) }
                request.jobs += scope.launch {
                    val late = try {
                        request.answer.await()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        null
                    }
                    if (late != RouteRequestResult.Started) return@launch
                    // 늦은 시작 알림도 목적지를 대조 — 사용자가 내비에서 다른 곳을 직접 정해 시작한 알림일 수 있음(심사 N1)
                    if (naviIsHeadingTo(request)) markStarted(request) else forget(request)
                }
                // TIMEOUT 뒤 기억에 기한 — 지나면 늦은 알림 · 재개 대조를 더 받지 않음(리스너 제거)
                request.jobs += scope.launch {
                    delay(AppConfig.TIMED_OUT_REQUEST_MEMORY_MILLIS)
                    forget(request)
                }
            }
            result is RouteRequestResult.Failed -> {
                lastRequest = null
                publish { GuideRequestState.Failed(result.reason, request.place, request.attempt, it) }
                request.stop()
            }
        }
    }

    /** TIMEOUT 뒤 기억한 요청을 버림 — 결과(TIMEOUT 문구)는 그대로 · 리스너 · 대기를 거둠. */
    private fun forget(request: GuideRequest) {
        if (lastRequest !== request || !request.timedOut) return
        lastRequest = null
        request.stop()
    }

    /** 바로 앞 요청이 시작됨 — 요청마다 한 번만(결과 · 핀 1건 · 기록 중일 때만은 핀 저장소가 봄 · T92 · T93). */
    private fun markStarted(request: GuideRequest) {
        if (lastRequest !== request) return
        lastRequest = null
        lastStartedPlace = request.place
        lastStartedAt = timeSource.nowMillis()
        publish { GuideRequestState.Started(request.place, it) }
        scope.launch { recordPin(request.place.id) }
        // 기다리던 응답 · 한도 · 늦은 알림 대기를 거둠(내비 리스너 제거) — 같은 요청이 다시 정해지지 않음
        request.stop()
    }

    /**
     * 내비가 지금 [request] 의 장소로 안내 중인가 — 조회 실패 · 한도 지남이면 아님.
     * 기다리는 사이 그 요청이 이미 정해졌거나 새 요청으로 바뀌었으면 조회하지 않음.
     */
    private suspend fun naviIsHeadingTo(request: GuideRequest): Boolean = try {
        // 얼림이 풀린 직후 쌓인 콜백(늦은 onRouteStarted 포함)이 먼저 지나가게 — 그 사이 조회는 옛 상태를 받을 수 있음
        delay(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS)
        lastRequest === request &&
            withTimeoutOrNull(AppConfig.SDK_CALL_TIMEOUT_MS) { navi.activeRoute() }
                ?.isHeadingTo(request.place) == true
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        // 조회 실패 = 안내 중 아님(실패 이유 로그는 data 쪽 몫 · domain 은 android Log 를 쓰지 않음)
        false
    }

    /** 핀 남기기 · 보내기 — 요청 상태에 영향 없음(navigation 4.6) · 실패해도 앱은 그대로. */
    private suspend fun recordPin(placeId: String) {
        try {
            recorder.recordGuidanceStart(placeId)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // 핀 저장소가 자기 실패를 로그로 남김(코드 · 상태만) — 여기선 삼킴
        }
    }

    private companion object {
        val TIMEOUT = RouteRequestResult.Failed(RouteFailReason.TIMEOUT)
    }
}
