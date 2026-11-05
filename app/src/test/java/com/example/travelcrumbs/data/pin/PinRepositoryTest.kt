package com.example.travelcrumbs.data.pin

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.PinUploadState
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.RecordingStatus
import com.example.travelcrumbs.domain.uploadRetryDelayMillis
import com.example.travelcrumbs.testing.FakeAccessTokenSource
import com.example.travelcrumbs.testing.FakeAppApi
import com.example.travelcrumbs.testing.FakePinLocalDataSource
import com.example.travelcrumbs.testing.FakePinUploadScheduler
import com.example.travelcrumbs.testing.FakeRecordingStatusRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.pinRepositoryOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 핀 남기기 · 순서 · 서버 보내기(스펙 navigation 4.1 · 4.6) — fake 기기 저장 · fake AppApi · fake 시계 · fake 예약. */
@OptIn(ExperimentalCoroutinesApi::class)
class PinRepositoryTest {

    private val recording = FakeRecordingStatusRepository(RecordingProgress(RecordingStatus.RECORDING, 0))
    private val local = FakePinLocalDataSource()
    private val api = FakeAppApi()
    private val tokens = FakeAccessTokenSource()
    private val time = FakeTimeSource(now = 1_762_300_800_000L)
    private val scheduler = FakePinUploadScheduler()

    private fun repository() = pinRepositoryOf(local, api, tokens, recording, time, scheduler)

    @Test
    fun `T43 기록 중 · 핀 0건에 시작 알림 — 핀 1건(장소 id · 시각 · 순서 1)`() = runTest {
        val repo = repository()

        assertTrue(repo.recordGuidanceStart("a"))

        val pin = repo.pins.first().single()
        assertEquals("a", pin.placeId)
        assertEquals(1, pin.sequence)
        assertEquals(time.now, pin.startedAtMillis)
    }

    @Test
    fun `T45 서버 받지 못함 · 대기 핀 3건에 1건 더 — 4건 모두 남고 대기 4건`() = runTest {
        api.postCode = null
        val repo = repository()
        repeat(4) { repo.recordGuidanceStart("a") }

        val pins = repo.pins.first()
        assertEquals(4, pins.size)
        assertTrue(pins.all { it.uploadState == PinUploadState.PENDING })
    }

    @Test
    fun `T46 받지 못함 · 핀 3건 — 앱 다시 실행(같은 기기 저장)해도 핀 3건 순서 대기 그대로`() = runTest {
        api.postCode = null
        val first = repository()
        listOf("a", "b", "c").forEach { first.recordGuidanceStart(it) }

        val again = repository()

        val pins = again.pins.first()
        assertEquals(listOf("a", "b", "c"), pins.map { it.placeId })
        assertEquals(listOf(1, 2, 3), pins.map { it.sequence })
        assertTrue(pins.all { it.uploadState == PinUploadState.PENDING })
    }

    @Test
    fun `T61 서버가 받음 201 — POST 1회(placeId · 시간대 있는 startedAt) · 끝 · 핀은 남음`() = runTest {
        val repo = repository()

        repo.recordGuidanceStart("a")

        assertEquals(1, api.posted.size)
        assertEquals("a", api.posted.single().placeId)
        assertTrue(
            "시간대가 붙은 ISO-8601: ${api.posted.single().startedAt}",
            Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})$""")
                .matches(api.posted.single().startedAt),
        )
        assertEquals(PinUploadState.SENT, repo.pins.first().single().uploadState)
        assertTrue(scheduler.retries.isEmpty())
    }

    @Test
    fun `T61 이미 받은 기록 200 도 끝`() = runTest {
        api.postCode = 200
        val repo = repository()

        repo.recordGuidanceStart("a")

        assertEquals(PinUploadState.SENT, repo.pins.first().single().uploadState)
    }

    @Test
    fun `T62 네트워크 없음 — 대기 · 다시 보내기 예약 · 연결되면 1회 보내고 끝`() = runTest {
        api.postCode = null
        val repo = repository()

        repo.recordGuidanceStart("a")
        assertEquals(PinUploadState.PENDING, repo.pins.first().single().uploadState)
        assertEquals(listOf(0), scheduler.retries)

        api.postCode = 201
        assertEquals(UploadRun.DONE, repo.uploadPending(retryIndex = 1))
        assertEquals(1, api.posted.size)
        assertEquals(PinUploadState.SENT, repo.pins.first().single().uploadState)
    }

    @Test
    fun `T63 5xx 가 이어짐 — 간격 30초부터 두 배 · 최대 1시간 · 받으면 끝`() = runTest {
        api.postCode = 503
        val repo = repository()
        repo.recordGuidanceStart("a")
        // 예약 작업이 retryIndex 를 하나씩 늘려 부르는 것을 흉내
        for (index in 1..10) repo.uploadPending(retryIndex = index)

        assertEquals((0..10).toList(), scheduler.retries)
        val delays = scheduler.retries.map { uploadRetryDelayMillis(it) / 1_000 }
        assertEquals(listOf(30L, 60L, 120L, 240L, 480L, 960L, 1920L, 3600L, 3600L, 3600L, 3600L), delays)

        api.postCode = 201
        assertEquals(UploadRun.DONE, repo.uploadPending(retryIndex = 11))
        assertEquals(11, scheduler.retries.size)
    }

    @Test
    fun `T63 시간 초과(받지 못함)도 다시 보냄 대상`() = runTest {
        api.postCode = 504
        val repo = repository()

        repo.recordGuidanceStart("a")

        assertEquals(listOf(0), scheduler.retries)
        assertEquals(PinUploadState.PENDING, repo.pins.first().single().uploadState)
    }

    @Test
    fun `T64 422 — 거절 · 다시 보내지 않음 · 핀은 남음`() = runTest {
        api.postCode = 422
        val repo = repository()

        repo.recordGuidanceStart("a")
        repo.uploadPending()

        assertEquals(1, api.posted.size)
        assertEquals(PinUploadState.REJECTED, repo.pins.first().single().uploadState)
        assertTrue(scheduler.retries.isEmpty())
    }

    @Test
    fun `T65 토큰 없음 · 401 · 403 — 보내지 않거나 더 보내지 않고 대기 · 간격 예약 없음 · 핀 남음`() = runTest {
        tokens.token = null
        val repo = repository()
        repo.recordGuidanceStart("a")
        assertEquals(0, api.posted.size)

        tokens.token = "t"
        api.postCode = 401
        assertEquals(UploadRun.WAITING_FOR_AUTH, repo.uploadPending())
        api.postCode = 403
        assertEquals(UploadRun.WAITING_FOR_AUTH, repo.uploadPending())

        assertEquals(PinUploadState.PENDING, repo.pins.first().single().uploadState)
        assertTrue(scheduler.retries.isEmpty())
    }

    @Test
    fun `T66 받지 못함 · 대기 2건 — 다시 실행 뒤 받음이면 2건 보내고 끝 · 핀 2건 그대로`() = runTest {
        api.postCode = null
        val first = repository()
        first.recordGuidanceStart("a")
        first.recordGuidanceStart("b")

        api.postCode = 201
        val again = repository()
        assertEquals(UploadRun.DONE, again.uploadPending())

        assertEquals(listOf("a", "b"), api.posted.map { it.placeId })
        val pins = again.pins.first()
        assertEquals(2, pins.size)
        assertTrue(pins.all { it.uploadState == PinUploadState.SENT })
    }

    @Test
    fun `T80 일시정지 — 핀 0건`() = runTest {
        recording.set(RecordingProgress(RecordingStatus.PAUSED, 3))
        val repo = repository()

        assertFalse(repo.recordGuidanceStart("a"))

        assertTrue(repo.pins.first().isEmpty())
        assertEquals(0, api.posted.size)
    }

    @Test
    fun `T81 기록 상태를 아직 못 읽음 — 한도 뒤 핀 0건`() = runTest {
        recording.set(null)
        val repo = repository()

        val result = async { repo.recordGuidanceStart("a") }
        advanceTimeBy(AppConfig.RECORDING_STATUS_READ_TIMEOUT_MILLIS + 1)
        runCurrent()

        assertFalse(result.await())
        assertTrue(repo.pins.first().isEmpty())
    }

    @Test
    fun `T82 기록 중 A → B → A — 핀 3건 · 순서 1 A 2 B 3 A`() = runTest {
        val repo = repository()

        listOf("a", "b", "a").forEach { repo.recordGuidanceStart(it) }

        val pins = repo.pins.first()
        assertEquals(listOf("a", "b", "a"), pins.map { it.placeId })
        assertEquals(listOf(1, 2, 3), pins.map { it.sequence })
    }

    @Test
    fun `T83 기록 중 A · 일시정지 B · 재개 C — 핀 2건 A(1) C(2) · 빈 번호 없음`() = runTest {
        val repo = repository()

        repo.recordGuidanceStart("a")
        recording.set(RecordingProgress(RecordingStatus.PAUSED, 1))
        repo.recordGuidanceStart("b")
        recording.set(RecordingProgress(RecordingStatus.RECORDING, 1))
        repo.recordGuidanceStart("c")

        val pins = repo.pins.first()
        assertEquals(listOf("a", "c"), pins.map { it.placeId })
        assertEquals(listOf(1, 2), pins.map { it.sequence })
    }

    @Test
    fun `T84 핀 저장소를 여행에 붙이는 fake 로 바꿔 끼움 — 인터페이스만으로 핀 1건을 받음`() = runTest {
        val tripPins = mutableListOf<String>()
        val attaching: PinRepository = object : PinRepository by repository() {
            override suspend fun recordGuidanceStart(placeId: String): Boolean {
                tripPins += placeId
                return true
            }
        }

        attaching.recordGuidanceStart("a")

        assertEquals(listOf("a"), tripPins)
    }
}
