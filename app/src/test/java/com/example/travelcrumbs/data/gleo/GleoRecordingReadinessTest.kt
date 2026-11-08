package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.consent.ConsentRepository
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.testing.FakeConsentRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeTripRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GleoRecordingReadinessTest {
    private fun trip(status: String = "RECORDING") = Trip(1L, "trip-one", "여행", status, emptyList())

    @Test
    fun `T6 T7 RECORDING과 PAUSED는 모두 기존 기록 준비 가능`() = runTest {
        listOf("RECORDING", "PAUSED").forEach { status ->
            val readiness = DefaultGleoRecordingReadiness(FakeTripRepository(trip(status)), FakeNaviRepository(), FakeConsentRepository(true))
            assertEquals(GleoReadiness.READY, readiness.check())
        }
    }

    @Test
    fun `T8 T11 초기 null과 확인된 부재를 구별하지 못하면 미준비로만 응답`() = runTest {
        val readiness = DefaultGleoRecordingReadiness(FakeTripRepository(), FakeNaviRepository(), FakeConsentRepository(true))
        assertEquals(GleoReadiness.NOT_READY, readiness.check())
    }

    @Test
    fun `T9 동의 N이나 미이력은 추가위치조회 전에 거절`() = runTest {
        val readiness = DefaultGleoRecordingReadiness(FakeTripRepository(trip()), FakeNaviRepository(), FakeConsentRepository(false))
        assertEquals(GleoReadiness.CONSENT_REQUIRED, readiness.check())
    }

    @Test
    fun `T10 동의 조회 실패를 미동의로 바꾸지 않음`() = runTest {
        val consent = object : ConsentRepository {
            override suspend fun isAgreed(type: ConsentType) = Result.failure<Boolean>(IllegalStateException())
            override suspend fun agree(type: ConsentType) = Result.success(Unit)
        }
        val readiness = DefaultGleoRecordingReadiness(FakeTripRepository(trip()), FakeNaviRepository(), consent)
        assertEquals(GleoReadiness.NOT_READY, readiness.check())
    }

    @Test
    fun `T12 내비가 연결되지 않았으면 위치 조회 대기를 시작하지 않음`() = runTest {
        val readiness = DefaultGleoRecordingReadiness(FakeTripRepository(trip()), FakeNaviRepository(ready = false), FakeConsentRepository(true))
        assertEquals(GleoReadiness.NOT_READY, readiness.check())
    }

    @Test
    fun `T11 동의를 확인하는 동안 여행이 끝나면 저장 준비를 철회`() = runTest {
        val trips = FakeTripRepository(trip())
        val consent = object : ConsentRepository {
            override suspend fun isAgreed(type: ConsentType): Result<Boolean> {
                trips.endTrip("trip-one")
                return Result.success(true)
            }
            override suspend fun agree(type: ConsentType) = Result.success(Unit)
        }
        val readiness = DefaultGleoRecordingReadiness(trips, FakeNaviRepository(), consent)
        assertEquals(GleoReadiness.NOT_READY, readiness.check())
    }
}
