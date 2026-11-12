package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.GleoStatus
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeRecordingStatusRepository
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GleoCommandProcessorTest {
    private class Fixture(private val scope: TestScope, writeEnabled: Boolean = true) {
        var readinessCalls = 0
        val requestIds = mutableListOf<UUID>()
        val recordCalls: Int get() = requestIds.size
        val issuedIds = mutableListOf<UUID>()
        var issueId: () -> UUID = UUID::randomUUID
        var ready: suspend ((Boolean) -> Unit) -> GleoReadiness = { GleoReadiness.READY }
        var record: suspend () -> ActionResult = { ActionResult.DONE }
        val callbacks = mutableListOf<GleoCallback>()
        val controlCalls = mutableListOf<String>()
        val infoCalls = mutableListOf<Pair<String, String?>>()
        var control: suspend (String) -> GleoOutcome = { GleoOutcome(GleoCommandPhase.TRIP_PAUSED, GleoStatus.SUCCESS, AppConfig.GLEO_PAUSED_MESSAGE) }
        var info: suspend (String, String?) -> GleoOutcome = { _, _ ->
            GleoOutcome(GleoCommandPhase.TODAY_RECORDS, GleoStatus.SUCCESS, "You recorded 1 place today.", "[]", "1", GleoScreenRequest.OpenTrips)
        }
        val processor = GleoCommandProcessor(
            scope.backgroundScope,
            GleoRecordingReadiness { restoring -> readinessCalls++; ready(restoring) },
            RecordPlaceUseCase { requestId -> requestIds += checkNotNull(requestId); record() },
            callbacks::add,
            writeEnabled,
            tripControl = { operation -> controlCalls += operation; control(operation) },
            travelInfo = { type, region -> infoCalls += type to region; info(type, region) },
            newRequestId = { issueId().also(issuedIds::add) },
        )

        fun handle(raw: String? = null, readable: Boolean = true) {
            processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_RECORD, raw, readable))
            scope.runCurrent()
        }
    }

    @Test
    fun `69-T1 여행 제어는 기록 준비 · 저장을 거치지 않고 파라미터를 그대로 넘김`() = runTest {
        val f = Fixture(this)
        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":"PAUSE"}"""))
        runCurrent()

        assertEquals(listOf("PAUSE"), f.controlCalls)
        assertEquals(AppConfig.GLEO_PAUSED_MESSAGE, f.callbacks.single().message)
        assertEquals(GleoStatus.SUCCESS, f.callbacks.single().status)
        assertEquals(GleoCommandPhase.TRIP_PAUSED, f.processor.state.value.phase)
        assertEquals(0, f.readinessCalls)
        assertEquals(0, f.recordCalls)
    }

    @Test
    fun `69-T2 여행 제어 파라미터가 없거나 enum 밖이면 입력 실패 · 처리기를 부르지 않음`() = runTest {
        val f = Fixture(this)
        for (raw in listOf("{}", """{"operation":"STOP"}""", """{"operation":"PAUSE","x":"1"}""")) {
            f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_CONTROL, raw))
            runCurrent()
        }
        assertTrue(f.controlCalls.isEmpty())
        assertEquals(3, f.callbacks.size)
        assertTrue(f.callbacks.all { it.status == GleoStatus.FAIL && it.message == AppConfig.GLEO_INPUT_FAIL_MESSAGE })
        assertEquals(GleoCommandPhase.INVALID_REQUEST, f.processor.state.value.phase)
    }

    @Test
    fun `69-T13 정보 조회 결과의 data · 화면 요청이 콜백과 상태에 실림`() = runTest {
        val f = Fixture(this)
        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"RECOMMENDED_ROADMAPS","region":"경주"}"""))
        runCurrent()

        assertEquals(listOf("RECOMMENDED_ROADMAPS" to "경주"), f.infoCalls)
        assertEquals("[]", f.callbacks.single().data)
        assertEquals(GleoScreenRequest.OpenTrips, f.processor.state.value.screen)
        assertEquals("1", f.processor.state.value.detail)
    }

    @Test
    fun `69-T14 다른 명령 처리 중 들어온 명령은 즉시 BUSY 실패 · 처리 중 명령은 그대로 끝남`() = runTest {
        val f = Fixture(this)
        val gate = CompletableDeferred<ActionResult>()
        f.record = { gate.await() }
        f.handle()
        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":"PAUSE"}"""))
        runCurrent()

        assertTrue(f.controlCalls.isEmpty())
        assertEquals(AppConfig.GLEO_BUSY_MESSAGE, f.callbacks.single().message)
        assertEquals(GleoStatus.FAIL, f.callbacks.single().status)

        gate.complete(ActionResult.DONE)
        runCurrent()
        assertEquals(GleoCommandPhase.SAVED, f.processor.state.value.phase)
        assertEquals(2, f.callbacks.size)
    }

    @Test
    fun `69 정보 조회가 7초를 넘기면 한 번만 실패로 답하고 늦은 결과는 버림`() = runTest {
        val f = Fixture(this)
        val gate = CompletableDeferred<Unit>()
        f.info = { _, _ ->
            gate.await()
            GleoOutcome(GleoCommandPhase.TODAY_RECORDS, GleoStatus.SUCCESS, "late", screen = GleoScreenRequest.OpenTrips)
        }
        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"TODAY_RECORDS"}"""))
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS + 1)
        runCurrent()
        assertEquals(AppConfig.GLEO_TIMEOUT_MESSAGE, f.callbacks.single().message)

        gate.complete(Unit)
        runCurrent()
        assertEquals(1, f.callbacks.size)
        assertEquals(GleoCommandPhase.REQUEST_FAILED, f.processor.state.value.phase)
        assertEquals(null, f.processor.state.value.screen)
    }

    @Test
    fun `T2 T3 미등록 명령은 업무와 콜백 모두 없음`() = runTest {
        val f = Fixture(this)
        f.processor.handle(GleoIntentRequest("android.intent.action.MAIN", null))
        f.processor.handle(GleoIntentRequest("OTHER_FUNCTION", null))

        assertTrue(f.callbacks.isEmpty())
        assertEquals(0, f.recordCalls)
        assertEquals(GleoCommandPhase.IDLE, f.processor.state.value.phase)
    }

    @Test
    fun `T4 무파라미터의 없음 빈값 빈객체는 같은 업무에 연결`() = runTest {
        val f = Fixture(this)
        listOf(null, "", " ", "{}", " { } ").forEach { f.handle(it) }

        assertEquals(5, f.recordCalls)
        assertEquals(5, f.callbacks.size)
        assertTrue(f.callbacks.all { it.status == GleoStatus.SUCCESS })
    }

    @Test
    fun `T5 잘못된 JSON 업무 파라미터 과대입력 extras실패는 준비 조회 전 거절`() = runTest {
        val f = Fixture(this)
        listOf("[]", "1", "null", "\"{}\"", "{bad}", "{\"tripUuid\":\"x\"}", " ".repeat(AppConfig.GLEO_MAX_ACTION_PARAM_CHARS + 1))
            .forEach { f.handle(it) }
        f.handle(readable = false)

        assertEquals(0, f.readinessCalls)
        assertEquals(0, f.recordCalls)
        assertEquals(8, f.callbacks.size)
        assertTrue(f.callbacks.all { it.status == GleoStatus.FAIL && it.message == AppConfig.GLEO_RECORD_INPUT_FAIL_MESSAGE })
    }

    @Test
    fun `T6 T15 저장 결과 전에는 성공을 보내지 않고 DONE 확인 뒤에만 성공`() = runTest {
        val f = Fixture(this)
        val saved = CompletableDeferred<ActionResult>()
        f.record = { saved.await() }
        f.handle()
        runCurrent()

        assertEquals(GleoCommandPhase.PROCESSING, f.processor.state.value.phase)
        assertTrue(f.callbacks.isEmpty())
        saved.complete(ActionResult.DONE)
        runCurrent()

        assertEquals(1, f.recordCalls)
        assertEquals(GleoCommandPhase.SAVED, f.processor.state.value.phase)
        assertEquals(GleoStatus.SUCCESS, f.callbacks.single().status)
    }

    @Test
    fun `T9 동의가 확인되지 않으면 기존 장소 기록 호출 없음`() = runTest {
        val f = Fixture(this)
        f.ready = { GleoReadiness.CONSENT_REQUIRED }
        f.handle()

        assertEquals(0, f.recordCalls)
        assertEquals(GleoStatus.FAIL, f.callbacks.single().status)
        assertEquals(AppConfig.GLEO_RECORD_CONSENT_MESSAGE, f.callbacks.single().message)
    }

    @Test
    fun `T10 T11 T12 미준비나 준비 조회 실패는 여행없음 또는 미동의로 단정하지 않음`() = runTest {
        val f = Fixture(this)
        f.ready = { GleoReadiness.NOT_READY }
        f.handle()
        assertEquals(AppConfig.GLEO_RECORD_NOT_READY_MESSAGE, f.callbacks.single().message)
        f.ready = { error("consent query failed") }
        f.handle()

        assertEquals(0, f.recordCalls)
        assertEquals(AppConfig.GLEO_RECORD_UNCONFIRMED_MESSAGE, f.callbacks.last().message)
        assertTrue(f.callbacks.all { it.status == GleoStatus.FAIL })
    }

    @Test
    fun `T13 T14 T17 확인된 실패와 결과 미확인을 구분`() = runTest {
        val f = Fixture(this)
        f.record = { ActionResult.FAILED }
        f.handle()
        assertEquals(GleoCommandPhase.FAILED, f.processor.state.value.phase)
        assertEquals(AppConfig.GLEO_RECORD_FAIL_MESSAGE, f.callbacks.single().message)
        f.record = { ActionResult.UNCONFIRMED }
        f.handle()
        assertEquals(GleoCommandPhase.UNCONFIRMED, f.processor.state.value.phase)
        f.record = { error("server response lost") }
        f.handle()

        assertEquals(3, f.recordCalls)
        assertTrue(f.callbacks.all { it.status == GleoStatus.FAIL })
        assertTrue(f.callbacks.drop(1).all { it.message == AppConfig.GLEO_RECORD_UNCONFIRMED_MESSAGE })
    }

    @Test
    fun `T11 초기 여행 복원 중 로딩을 보여주고 준비된 뒤 한 번 저장`() = runTest {
        val f = Fixture(this)
        // develop: 기기 안 여행을 처음 읽는 동안(아직 값 없음)을 복원 중으로 봄.
        val recording = FakeRecordingStatusRepository(null)
        val users = FakeCurrentUserRepository().apply { recordConsent(ConsentType.LOCATION_COLLECT, true) }
        f.ready = DefaultGleoRecordingReadiness(recording, users, FakeNaviRepository())::check
        f.handle()
        f.processor.clearNotice(f.processor.state.value.noticeId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(GleoCommandPhase.LOADING_TRIP, f.processor.state.value.phase)
        assertEquals(0, f.recordCalls)
        assertTrue(f.callbacks.isEmpty())

        recording.set(RecordingProgress(status = TripStatus.RECORDING, elapsedMinutes = 0, tripNo = 1L))
        runCurrent()
        assertEquals(1, f.recordCalls)
        assertEquals(GleoCommandPhase.SAVED, f.processor.state.value.phase)
        assertEquals(GleoStatus.SUCCESS, f.callbacks.single().status)
    }

    @Test
    fun `T11 시작 복원 뒤 진행 중 여행이 없으면 저장 없이 미준비 안내`() = runTest {
        val f = Fixture(this)
        val recording = FakeRecordingStatusRepository(null)
        val users = FakeCurrentUserRepository().apply { recordConsent(ConsentType.LOCATION_COLLECT, true) }
        f.ready = DefaultGleoRecordingReadiness(recording, users, FakeNaviRepository())::check
        f.handle()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(GleoCommandPhase.LOADING_TRIP, f.processor.state.value.phase)
        assertTrue(f.callbacks.isEmpty())

        recording.set(RecordingProgress.NONE)
        runCurrent()
        assertEquals(0, f.recordCalls)
        assertEquals(GleoCommandPhase.NOT_READY, f.processor.state.value.phase)
        assertEquals(AppConfig.GLEO_RECORD_NOT_READY_MESSAGE, f.callbacks.single().message)
    }

    @Test
    fun `T11 시작 복원 읽기 실패는 미확인으로 종료하고 장소 저장을 시작하지 않음`() = runTest {
        val f = Fixture(this)
        val broken = object : RecordingRepository by FakeRecordingStatusRepository(null) {
            override val progress: Flow<RecordingProgress> = flow {
                delay(1_000L)
                throw IllegalStateException("기기 안 여행 읽기 실패")
            }
        }
        val users = FakeCurrentUserRepository().apply { recordConsent(ConsentType.LOCATION_COLLECT, true) }
        f.ready = DefaultGleoRecordingReadiness(broken, users, FakeNaviRepository())::check
        f.handle()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(GleoCommandPhase.LOADING_TRIP, f.processor.state.value.phase)

        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(f.issuedIds.isEmpty())
        assertEquals(0, f.recordCalls)
        assertEquals(GleoCommandPhase.UNCONFIRMED, f.processor.state.value.phase)
        assertEquals(AppConfig.GLEO_RECORD_UNCONFIRMED_MESSAGE, f.callbacks.single().message)
    }

    @Test
    fun `T11a T16 7초는 준비 조회부터 포함하고 기한 뒤 준비되어도 저장을 시작하지 않음`() = runTest {
        val f = Fixture(this)
        val ready = CompletableDeferred<GleoReadiness>()
        f.ready = { restoring -> restoring(true); ready.await() }
        f.handle()
        advanceTimeBy(6_999L)
        runCurrent()
        assertEquals(GleoCommandPhase.LOADING_TRIP, f.processor.state.value.phase)
        assertTrue(f.callbacks.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(AppConfig.GLEO_RECORD_TIMEOUT_MESSAGE, f.callbacks.single().message)
        assertEquals(GleoCommandPhase.UNCONFIRMED, f.processor.state.value.phase)
        assertTrue(f.issuedIds.isEmpty())

        // 첫 준비 조회가 응답하지 않아도 별도 명령을 영구히 막지 않음.
        f.ready = { GleoReadiness.READY }
        f.handle()
        assertEquals(1, f.recordCalls)
        assertEquals(GleoStatus.SUCCESS, f.callbacks.last().status)

        ready.complete(GleoReadiness.READY)
        runCurrent()
        assertEquals(1, f.recordCalls)
        assertEquals(2, f.callbacks.size)
    }

    @Test
    fun `T18 T19 기한이 지나도 실제 저장 종료까지 중복을 합치고 늦은성공 콜백 없음`() = runTest {
        val f = Fixture(this)
        val saved = CompletableDeferred<ActionResult>()
        f.record = { saved.await() }
        f.handle()
        f.handle()
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
        runCurrent()
        assertEquals(1, f.recordCalls)
        assertEquals(1, f.issuedIds.size)
        assertEquals(GleoStatus.FAIL, f.callbacks.single().status)

        f.handle()
        assertEquals(1, f.recordCalls)
        saved.complete(ActionResult.DONE)
        runCurrent()
        assertEquals(1, f.callbacks.size)
        assertEquals(GleoCommandPhase.UNCONFIRMED, f.processor.state.value.phase)

        f.record = { ActionResult.DONE }
        f.handle()
        assertEquals(2, f.recordCalls)
        assertEquals(f.issuedIds, f.requestIds)
        assertEquals(2, f.requestIds.distinct().size)
        assertEquals(GleoStatus.SUCCESS, f.callbacks.last().status)
    }

    @Test
    fun `T18 처리기한은 업무를 취소하지 않아 취소가 실패로 변환되는 기존경로도 재실행 안함`() = runTest {
        val f = Fixture(this)
        var cancelled = false
        f.record = {
            runCatching { delay(AppConfig.GLEO_RESPONSE_TARGET_MILLIS * 2) }
                .onFailure { cancelled = true }
            ActionResult.DONE
        }
        f.handle()
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
        runCurrent()
        f.handle()
        assertEquals(false, cancelled)
        assertEquals(1, f.recordCalls)
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
        runCurrent()
        assertEquals(false, cancelled)
        assertEquals(1, f.callbacks.size)
    }

    @Test
    fun `T16 결과와 기한이 같은 시각이어도 콜백은 한 번`() = runTest {
        val f = Fixture(this)
        f.record = { delay(AppConfig.GLEO_RESPONSE_TARGET_MILLIS); ActionResult.DONE }
        f.handle()
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
        runCurrent()

        assertEquals(1, f.callbacks.size)
        assertEquals(1, f.recordCalls)
    }

    @Test
    fun `T25 표시 해제는 진행중 업무와 콜백을 바꾸지 않고 오래된 ID는 새결과를 지우지 않음`() = runTest {
        val f = Fixture(this)
        val saved = CompletableDeferred<ActionResult>()
        f.record = { saved.await() }
        f.handle()
        val firstId = f.processor.state.value.noticeId
        f.processor.clearNotice(firstId)
        assertEquals(GleoCommandPhase.PROCESSING, f.processor.state.value.phase)
        saved.complete(ActionResult.DONE)
        runCurrent()
        f.processor.clearNotice(firstId)
        assertEquals(GleoCommandPhase.IDLE, f.processor.state.value.phase)

        f.record = { ActionResult.DONE }
        f.handle()
        f.processor.clearNotice(firstId)
        assertEquals(GleoCommandPhase.SAVED, f.processor.state.value.phase)
        assertEquals(2, f.recordCalls)
        assertEquals(2, f.callbacks.size)
    }

    @Test
    fun `T27 콜백 전송 실패는 저장 재시도나 두번째 콜백 시도를 만들지 않음`() = runTest {
        var records = 0
        var callbacks = 0
        val processor = GleoCommandProcessor(
            backgroundScope,
            { GleoReadiness.READY },
            { records++; ActionResult.DONE },
            { callbacks++; error("broadcast failed") },
            writeEnabled = true,
            tripControl = { error("not used") },
            travelInfo = { _, _ -> error("not used") },
        )
        processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_RECORD, null))
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
        runCurrent()

        assertEquals(1, records)
        assertEquals(1, callbacks)
        assertEquals(GleoCommandPhase.SAVED, processor.state.value.phase)
    }

    @Test
    fun `T35 요청 ID를 만들 수 없으면 장소 저장에 진입하지 않음`() = runTest {
        val f = Fixture(this)
        f.issueId = { error("UUID generation failed") }
        f.handle()

        assertEquals(0, f.recordCalls)
        assertEquals(GleoCommandPhase.FAILED, f.processor.state.value.phase)
        assertEquals(AppConfig.GLEO_RECORD_FAIL_MESSAGE, f.callbacks.single().message)
    }

    @Test
    fun `T37 69-U1 배포용 설정은 RECORD · CONTROL 을 차단하고 정보 조회만 받음`() = runTest {
        val f = Fixture(this, writeEnabled = false)
        f.handle()

        assertEquals(0, f.readinessCalls)
        assertEquals(0, f.recordCalls)
        assertTrue(f.issuedIds.isEmpty())
        assertTrue(f.callbacks.isEmpty())
        assertEquals(GleoCommandPhase.IDLE, f.processor.state.value.phase)

        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":"PAUSE"}"""))
        runCurrent()
        assertTrue(f.controlCalls.isEmpty())
        assertTrue(f.callbacks.isEmpty())

        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"TODAY_RECORDS"}"""))
        runCurrent()
        assertEquals(GleoStatus.SUCCESS, f.callbacks.single().status)
    }
}
