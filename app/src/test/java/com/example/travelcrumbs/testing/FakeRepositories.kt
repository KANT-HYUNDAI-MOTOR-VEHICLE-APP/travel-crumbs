package com.example.travelcrumbs.testing

import android.content.Intent
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.GuidanceStartRecorder
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.NaviEvent
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteRequestResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** mock 라이브러리를 쓰지 않음 — 테스트 대역은 손으로 만든 fake 하나씩. */
class FakeDrivingSignalRepository(
    initial: DrivingState = DrivingState.UNKNOWN,
) : DrivingSignalRepository {

    private val state = MutableStateFlow(initial)

    override val drivingState: Flow<DrivingState> = state

    fun emit(value: DrivingState) {
        state.value = value
    }
}

class FakeNaviRepository(
    private val result: Result<NaviRouteState> = Result.failure(IllegalStateException("준비 안 됨")),
    ready: Boolean = true,
) : NaviRepository {

    private val readyState = MutableStateFlow(ready)

    override val ready: StateFlow<Boolean> = readyState

    override suspend fun routeState(): NaviRouteState = result.getOrThrow()

    /** 내비 앱이 있는 기기 흉내(#51 U0) — 거짓이면 「내비 보기」 없음. */
    var naviApp: Boolean = true

    /** 「내비 보기」 · 목적지 없는 여행 시작으로 내비 앱을 연 횟수. */
    var naviAppOpens: Int = 0
        private set

    /** 지금 위치(#51 U4) — null 이면 못 읽음. */
    var location: com.example.travelcrumbs.domain.model.CurrentLocation? =
        com.example.travelcrumbs.domain.model.CurrentLocation(38.2049, 128.5906, "강원 속초시 중앙로 147")

    override suspend fun currentLocation(): com.example.travelcrumbs.domain.model.CurrentLocation? = location

    override fun naviAppAvailable(): Boolean = naviApp

    override fun openNaviApp(): Boolean {
        if (!naviApp) return false
        naviAppOpens++
        return true
    }

    /** 길안내 요청을 받은 장소들(#4) — 요청 건수를 셈. */
    val routeRequests = mutableListOf<Place>()

    /** 요청 호출이 던질 예외(T15) — null 이면 [answerRoute] 를 기다림. */
    var routeError: Throwable? = null

    private var pendingAnswer = CompletableDeferred<RouteRequestResult>()

    override suspend fun requestRoute(place: Place): RouteRequestResult {
        routeRequests += place
        routeError?.let { throw it }
        return pendingAnswer.await()
    }

    /** 내비가 시작 · 오류를 알림 — 기다리던 요청에 답하고 다음 요청은 새로 기다림. */
    fun answerRoute(result: RouteRequestResult) {
        val answer = pendingAnswer
        pendingAnswer = CompletableDeferred()
        answer.complete(result)
    }

    fun setReady(value: Boolean) {
        readyState.value = value
    }

    /** 지금 안내 상태(R1 대조) — 기본은 안내 중 아님. */
    var activeRoute: NaviActiveRoute = NaviActiveRoute(routing = false, destinationName = "", 0.0, 0.0)
    var activeRouteQueries: Int = 0
        private set

    /** 조회가 던질 예외(T90) — null 이면 [activeRoute] 를 돌려줌. */
    var activeRouteError: Throwable? = null

    /** 조회 응답을 늦춤(T90 조회 한도) — 완료되기 전엔 답하지 않음. */
    var activeRouteGate: CompletableDeferred<Unit>? = null

    override suspend fun activeRoute(): NaviActiveRoute {
        activeRouteQueries++
        activeRouteGate?.await()
        activeRouteError?.let { throw it }
        return activeRoute
    }

    /** 내비 상시 알림(#66) — 시험이 [naviEvents] 로 보냄. */
    val naviEvents = MutableSharedFlow<NaviEvent>(extraBufferCapacity = 64)

    override val events: Flow<NaviEvent> = naviEvents

    /** 시작 알림 때 조회할 안내 중 목적지(#66) — null 이면 안내 중 아님 · 실패. */
    var destination: NaviDestination? = null
    var destinationQueries: Int = 0
        private set

    /** 목적지 조회 응답을 늦춤(T46) — 완료되기 전엔 답하지 않음. */
    var destinationGate: CompletableDeferred<Unit>? = null

    override suspend fun guidedDestination(): NaviDestination? {
        destinationQueries++
        destinationGate?.await()
        return destination
    }

    /** 내비가 [place] 로 안내 중인 상태로 둠. */
    fun guidingTo(place: Place) {
        activeRoute = NaviActiveRoute(true, place.name, place.latitude, place.longitude)
    }
}

class FakeGleoActionRepository(
    opened: Boolean = false,
) : GleoActionRepository {

    private val state = MutableStateFlow(opened)

    override val openedByGleoAction: StateFlow<Boolean> = state
    override val commandState = MutableStateFlow(GleoCommandState())

    var handledCount: Int = 0
        private set

    /** 단위 테스트에서는 Intent 를 만들 수 없어 호출만 세어 둠. */
    override fun handleActionIntent(intent: Intent?) {
        handledCount++
    }

    override fun clearCommandNotice(noticeId: Long) {
        if (commandState.value.noticeId == noticeId) commandState.value = GleoCommandState()
    }
}

/**
 * 시험용 길안내 요청 입구 — 앱 수명 범위를 흉내 낸 자기 scope(메인 · 시험에선 setMain 의 시험 디스패처).
 * 새로 만들면 프로세스 재생성 흉내(요청 상태 대기 · T108).
 */
fun guideTrackerOf(
    navi: FakeNaviRepository,
    recorder: GuidanceStartRecorder,
    time: com.example.travelcrumbs.domain.TimeSource = FakeTimeSource(),
): GuideRequestTracker =
    GuideRequestTracker(navi, recorder, CoroutineScope(SupervisorJob() + Dispatchers.Main), time)
