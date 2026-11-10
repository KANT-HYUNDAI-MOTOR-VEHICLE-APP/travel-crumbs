package com.example.travelcrumbs.ui.recommendations.map

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationMapRuntimeTest {
    @Test
    fun `M02 M23 M24 M39 동의 정차 둘 다 필요 · 철회 즉시 불허`() = runTest {
        val users = FakeCurrentUserRepository()
        val driving = FakeDrivingSignalRepository(DrivingState.PARKED)
        var calls = 0
        val fake = FakeNaviRepository()
        val navi = object : NaviRepository by fake {
            override suspend fun currentLocation(): CurrentLocation? { calls++; return fake.location }
        }
        val runtime = RecommendationMapRuntime(users, driving, navi, FakeTripRepository())
        assertNull(runtime.locate(runtime.access.first()))
        assertEquals(0, calls)
        users.recordConsent(ConsentType.LOCATION_RECO, true)
        assertNotNull(runtime.locate(runtime.access.first()))
        assertEquals(1, calls)
        for (state in listOf(DrivingState.DRIVING, DrivingState.UNKNOWN)) {
            driving.emit(state)
            assertNull(runtime.locate(runtime.access.first()))
        }
        driving.emit(DrivingState.PARKED)
        users.recordConsent(ConsentType.LOCATION_RECO, false)
        assertNull(runtime.locate(runtime.access.first()))
        assertEquals(1, calls)
    }

    @Test
    fun `M25 위치 조회 취소 뒤 늦은 응답 폐기`() = runTest {
        val answer = CompletableDeferred<CurrentLocation?>()
        val navi = object : NaviRepository by FakeNaviRepository() {
            override suspend fun currentLocation(): CurrentLocation? = answer.await()
        }
        val runtime = RecommendationMapRuntime(FakeCurrentUserRepository(), FakeDrivingSignalRepository(), navi, FakeTripRepository())
        var delivered = false
        val query = launch { runtime.locate(MapAccess(true, true)); delivered = true }
        runCurrent()
        query.cancel()
        answer.complete(CurrentLocation(38.2, 128.5, ""))
        runCurrent()
        assertFalse(delivered)
    }
}
