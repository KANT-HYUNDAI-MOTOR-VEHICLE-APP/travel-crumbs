package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.llm.LlmRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteState
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

class FakeLlmRepository(
    private val result: Result<String> = Result.success("안녕하세요."),
) : LlmRepository {

    override suspend fun ask(prompt: String): String = result.getOrThrow()
}
