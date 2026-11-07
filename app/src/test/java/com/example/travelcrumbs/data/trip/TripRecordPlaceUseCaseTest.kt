package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.NaviLocation
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TripRecordPlaceUseCaseTest {

    private val recording = Trip(1L, "uuid-1", "2026.11.07 여행", "RECORDING", listOf("SOLO"))
    private val here = NaviLocation(37.7543, 128.899, "강원특별자치도 강릉시 성내동")

    private fun useCase(trips: FakeTripRepository, navi: FakeNaviRepository) =
        TripRecordPlaceUseCase(trips, navi, FakeTimeSource(now = 1_000L))

    @Test
    fun `기록 중이면 지금 위치를 임시 저장하고 장소 수가 오름`() = runTest {
        val trips = FakeTripRepository(current = recording)
        val result = useCase(trips, FakeNaviRepository(location = Result.success(here)))()

        assertEquals(ActionResult.DONE, result)
        assertEquals(1, trips.places.size)
        assertEquals(37.7543, trips.places.single().lat, 0.0)
        assertEquals(1, trips.activeTrip.value?.placeCount)
    }

    @Test
    fun `기록 중인 여행이 없으면 실패`() = runTest {
        val trips = FakeTripRepository(current = null)
        assertEquals(ActionResult.FAILED, useCase(trips, FakeNaviRepository(location = Result.success(here)))())
    }

    @Test
    fun `위치를 못 받거나 0,0 이면 저장하지 않음`() = runTest {
        val trips = FakeTripRepository(current = recording)
        assertEquals(ActionResult.FAILED, useCase(trips, FakeNaviRepository())())
        val zero = FakeNaviRepository(location = Result.success(NaviLocation(0.0, 0.0, null)))
        assertEquals(ActionResult.FAILED, useCase(trips, zero)())
        assertEquals(0, trips.places.size)
    }
}
