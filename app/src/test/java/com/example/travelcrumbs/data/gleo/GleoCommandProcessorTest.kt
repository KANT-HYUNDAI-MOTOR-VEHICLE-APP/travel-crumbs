package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GleoCommandProcessorTest {
    private class Fixture(scope: TestScope) {
        var readinessCalls = 0
        var recordCalls = 0
        var ready: suspend () -> GleoReadiness = { GleoReadiness.READY }
        var record: suspend () -> ActionResult = { ActionResult.DONE }
        val callbacks = mutableListOf<GleoCallback>()
        val processor = GleoCommandProcessor(
            scope.backgroundScope,
            GleoRecordingReadiness { readinessCalls++; ready() },
            RecordPlaceUseCase { recordCalls++; record() },
            callbacks::add,
        )

        fun handle(raw: String? = null, readable: Boolean = true) = processor.handle(
            GleoIntentRequest(AppConfig.GLEO_FUNCTION_RECORD, raw, readable),
        )
    }

    @Test
    fun `T1 OPEN은 기존 성공 응답만 보내고 기록 준비나 저장을 호출하지 않음`() = runTest {
        val f = Fixture(this)
        f.processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_OPEN, null))

        assertEquals(AppConfig.GLEO_OPEN_SUCCESS_MESSAGE, f.callbacks.single().message)
        assertEquals(GleoStatus.SUCCESS, f.callbacks.single().status)
        assertEquals(0, f.readinessCalls)
        assertEquals(0, f.recordCalls)
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
    fun `T14 T17 기존 FAILED나 예외는 원인을 만들어내지 않고 결과 미확인`() = runTest {
        val f = Fixture(this)
        f.record = { ActionResult.FAILED }
        f.handle()
        f.record = { error("server response lost") }
        f.handle()

        assertEquals(2, f.recordCalls)
        assertTrue(f.callbacks.all { it.message == AppConfig.GLEO_RECORD_UNCONFIRMED_MESSAGE && it.status == GleoStatus.FAIL })
    }

    @Test
    fun `T16 6초는 준비 조회부터 포함하고 기한 뒤 준비되어도 저장을 시작하지 않음`() = runTest {
        val f = Fixture(this)
        val ready = CompletableDeferred<GleoReadiness>()
        f.ready = { ready.await() }
        f.handle()
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS - 1)
        runCurrent()
        assertTrue(f.callbacks.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(AppConfig.GLEO_RECORD_TIMEOUT_MESSAGE, f.callbacks.single().message)

        ready.complete(GleoReadiness.READY)
        runCurrent()
        assertEquals(0, f.recordCalls)
        assertEquals(1, f.callbacks.size)
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
        )
        processor.handle(GleoIntentRequest(AppConfig.GLEO_FUNCTION_RECORD, null))
        advanceTimeBy(AppConfig.GLEO_RESPONSE_TARGET_MILLIS)
        runCurrent()

        assertEquals(1, records)
        assertEquals(1, callbacks)
        assertEquals(GleoCommandPhase.SAVED, processor.state.value.phase)
    }
}
