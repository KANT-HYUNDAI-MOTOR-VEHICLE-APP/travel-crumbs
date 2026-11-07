package com.example.travelcrumbs.testing

import android.content.Intent
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.user.UserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
import com.example.travelcrumbs.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
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

    override val ready: StateFlow<Boolean> = MutableStateFlow(ready)

    override suspend fun routeState(): NaviRouteState = result.getOrThrow()
}

class FakeGleoActionRepository(
    opened: Boolean = false,
) : GleoActionRepository {

    private val state = MutableStateFlow(opened)

    override val openedByGleoAction: StateFlow<Boolean> = state

    var handledCount: Int = 0
        private set

    /** 단위 테스트에서는 Intent 를 만들 수 없어 호출만 세어 둠. */
    override fun handleActionIntent(intent: Intent?) {
        handledCount++
    }
}

class FakeUserRepository(
    private val result: Result<UserProfile> = Result.success(UserProfile(userNo = 2L, nickname = "샘플여행자1")),
) : UserRepository {

    override suspend fun myProfile(): Result<UserProfile> = result
}
