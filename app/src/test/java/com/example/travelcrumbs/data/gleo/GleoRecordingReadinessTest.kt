package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.model.ConsentState
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeRecordingStatusRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** develop 의 기기 안 기록 구조(RecordingRepository · CurrentUserRepository)에 맞춘 준비 확인(#25). */
class GleoRecordingReadinessTest {
    private fun progress(status: TripStatus = TripStatus.RECORDING, tripNo: Long? = 1L) =
        RecordingProgress(status = status, elapsedMinutes = 0, tripNo = tripNo)

    private suspend fun agreedUsers(agreed: Boolean = true) =
        FakeCurrentUserRepository().apply { recordConsent(ConsentType.LOCATION_COLLECT, agreed) }

    @Test
    fun `T6 T7 RECORDING과 PAUSED는 모두 기존 기록 준비 가능`() = runTest {
        listOf(TripStatus.RECORDING, TripStatus.PAUSED).forEach { status ->
            val readiness = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress(status)), agreedUsers(), FakeNaviRepository())
            assertEquals(GleoReadiness.READY, readiness.check {})
        }
    }

    @Test
    fun `T8 진행 중 여행이 없거나 끝났으면 미준비`() = runTest {
        val none = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress(tripNo = null)), agreedUsers(), FakeNaviRepository())
        assertEquals(GleoReadiness.NOT_READY, none.check {})
        val ended = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress(TripStatus.COMPLETED)), agreedUsers(), FakeNaviRepository())
        assertEquals(GleoReadiness.NOT_READY, ended.check {})
    }

    @Test
    fun `T9 위치 동의 N이나 미이력은 기록 전에 거절`() = runTest {
        val denied = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress()), agreedUsers(false), FakeNaviRepository())
        assertEquals(GleoReadiness.CONSENT_REQUIRED, denied.check {})
        val never = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress()), FakeCurrentUserRepository(), FakeNaviRepository())
        assertEquals(GleoReadiness.CONSENT_REQUIRED, never.check {})
    }

    @Test
    fun `T10 동의 조회 실패를 결과 미확인으로 분리`() = runTest {
        val broken = object : CurrentUserRepository by FakeCurrentUserRepository() {
            override val consents: Flow<Map<ConsentType, ConsentState>> = flow { throw IllegalStateException("읽기 실패") }
        }
        val readiness = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress()), broken, FakeNaviRepository())
        assertEquals(GleoReadiness.UNCONFIRMED, readiness.check {})
    }

    @Test
    fun `T12 내비가 끝내 연결되지 않으면 미준비`() = runTest {
        val readiness = DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress()), agreedUsers(), FakeNaviRepository(ready = false))
        assertEquals(GleoReadiness.NOT_READY, readiness.check {})
    }

    @Test
    fun `T11 동의를 확인하는 동안 여행이 끝나면 저장 준비를 철회`() = runTest {
        val recording = FakeRecordingStatusRepository(progress())
        val users = object : CurrentUserRepository by FakeCurrentUserRepository() {
            override val consents: Flow<Map<ConsentType, ConsentState>> = flow {
                recording.set(progress(TripStatus.COMPLETED))
                emit(mapOf(ConsentType.LOCATION_COLLECT to ConsentState(ConsentType.LOCATION_COLLECT, true, 0L)))
            }
        }
        val readiness = DefaultGleoRecordingReadiness(recording, users, FakeNaviRepository())
        assertEquals(GleoReadiness.NOT_READY, readiness.check {})
    }

    @Test
    fun `T24 기기 안 여행을 처음 읽는 데 오래 걸리면 그동안만 불러오는 중 표시`() = runTest {
        val recording = FakeRecordingStatusRepository(null)
        val shown = mutableListOf<Boolean>()
        val readiness = DefaultGleoRecordingReadiness(recording, agreedUsers(), FakeNaviRepository())
        val result = async { readiness.check { shown += it } }
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()
        assertEquals(listOf(true), shown)

        recording.set(progress())
        assertEquals(GleoReadiness.READY, result.await())
        assertEquals(listOf(true, false), shown)
    }

    @Test
    fun `여행 상태를 곧바로 읽으면 불러오는 중 표시 없음`() = runTest {
        val shown = mutableListOf<Boolean>()
        DefaultGleoRecordingReadiness(FakeRecordingStatusRepository(progress()), agreedUsers(), FakeNaviRepository()).check { shown += it }
        assertTrue(shown.isEmpty())
    }
}
