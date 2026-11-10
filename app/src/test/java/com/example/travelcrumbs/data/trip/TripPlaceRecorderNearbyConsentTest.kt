package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeRecordingLocalDataSource
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.recordingRepositoryOf
import com.example.travelcrumbs.testing.unfinishedInputRepositoryOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** A-02 장소 기록 위치는 LOCATION_COLLECT 만 봄 — 근처 로드맵 위치 동의(LOCATION_RECO) 철회와 독립(#55 · 스펙 home-nearby H50 · 3-5). */
class TripPlaceRecorderNearbyConsentTest {

    @Test
    fun `H50 두 동의 Y 뒤 근처 위치 동의만 철회 — LOCATION_COLLECT 그대로 · 장소가 위치와 함께 기록`() = runTest {
        val time = FakeTimeSource()
        val users = FakeCurrentUserRepository(time = time)
        users.recordConsent(ConsentType.LOCATION_COLLECT, true)
        users.recordConsent(ConsentType.LOCATION_RECO, true)
        users.recordConsent(ConsentType.LOCATION_RECO, false)
        val trips = FakeTripRepository(
            initial = listOf(
                TripRecord(
                    tripNo = 7L,
                    tripNm = null,
                    status = TripStatus.RECORDING,
                    visibility = Visibility.PRIVATE,
                    startAt = time.now,
                    endAt = null,
                    durationMillis = 0L,
                    destPlaceNm = null,
                    companions = emptySet(),
                    places = emptyList(),
                ),
            ),
            time = time,
        )
        val recorder = TripPlaceRecorder(
            recordingRepositoryOf(time, FakeRecordingLocalDataSource(CurrentTrip.start(time.now).copy(tripNo = 7L))),
            trips,
            users,
            FakeNaviRepository(),
            FakeDrivingSignalRepository(DrivingState.PARKED),
            unfinishedInputRepositoryOf(time),
        )

        assertEquals(ActionResult.DONE, recorder())
        val place = trips.current.single().places.single()
        assertNotNull(place.lat)
        assertNotNull(place.lng)
    }
}
