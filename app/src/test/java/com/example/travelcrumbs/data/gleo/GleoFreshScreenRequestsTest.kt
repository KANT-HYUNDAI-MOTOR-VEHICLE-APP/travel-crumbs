package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.testing.FakeGleoActionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #69 Pleos 리뷰 — 늦게 만들어진 화면이 지난 Gleo 명령(종료 팝업 · 화면 이동)을 다시 따르지 않음. */
class GleoFreshScreenRequestsTest {
    private val now = 1_000_000L

    private fun end(id: Long, at: Long) =
        GleoCommandState(id, GleoCommandPhase.END_CONFIRM, screen = GleoScreenRequest.ConfirmEndTrip, issuedAtMillis = at)

    @Test
    fun `유효 시간 안의 요청은 따름`() = runTest {
        val repo = FakeGleoActionRepository().apply { commandState.value = end(1L, now - 1_000L) }
        assertEquals(1L, repo.freshScreenRequests { now }.first().noticeId)
    }

    @Test
    fun `유효 시간이 지난 요청 · 화면 요청 없는 상태는 따르지 않음`() = runTest {
        val repo = FakeGleoActionRepository().apply {
            commandState.value = end(1L, now - AppConfig.GLEO_SCREEN_REQUEST_FRESH_MILLIS - 1)
        }
        val seen = mutableListOf<Long>()
        val job = launch { repo.freshScreenRequests { now }.collect { seen += it.noticeId } }
        runCurrent()
        repo.commandState.value = GleoCommandState(2L, GleoCommandPhase.TRIP_PAUSED, issuedAtMillis = now)
        runCurrent()
        assertTrue(seen.isEmpty())

        repo.commandState.value = end(3L, now)
        runCurrent()
        assertEquals(listOf(3L), seen)
        job.cancel()
    }

    @Test
    fun `같은 명령은 한 번만`() = runTest {
        val repo = FakeGleoActionRepository().apply { commandState.value = end(5L, now) }
        val seen = mutableListOf<Long>()
        val job = launch { repo.freshScreenRequests { now }.collect { seen += it.noticeId } }
        runCurrent()
        repo.commandState.value = end(5L, now).copy(detail = "x")
        runCurrent()
        assertEquals(listOf(5L), seen)
        job.cancel()
    }
}
