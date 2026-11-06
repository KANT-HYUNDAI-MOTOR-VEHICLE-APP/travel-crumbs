package com.example.travelcrumbs.data.draft

import com.example.travelcrumbs.domain.model.Draft
import com.example.travelcrumbs.testing.FakeDraftLocalDataSource
import com.example.travelcrumbs.testing.FakeTimeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftRepositoryTest {

    private val local = FakeDraftLocalDataSource()
    private val repository = DefaultDraftRepository(local, FakeTimeSource(now = 42L))

    @Test
    fun `T21 입력 중 내용이 있으면 저장`() = runTest {
        repository.updateActiveInput("trips", mapOf("memo" to "바다"))

        repository.saveActiveInput()

        assertEquals(Draft("trips", mapOf("memo" to "바다"), 42L), repository.savedDraft.first())
    }

    @Test
    fun `T24 입력 중 내용이 없거나 비어 있으면 저장 안 함`() = runTest {
        repository.saveActiveInput()
        assertNull(local.current)

        repository.updateActiveInput("trips", mapOf("memo" to "  "))
        repository.saveActiveInput()
        assertNull(local.current)

        repository.updateActiveInput("trips", mapOf("memo" to "x"))
        repository.clearActiveInput("trips")
        repository.saveActiveInput()
        assertNull(local.current)
    }

    @Test
    fun `T23 이어서 쓰기 — 복원 상태로 옮기고 저장은 비움 · 받은 화면이 지움`() = runTest {
        repository.updateActiveInput("trips", mapOf("memo" to "바다"))
        repository.saveActiveInput()

        val taken = repository.takeForRestore()

        assertEquals("trips", taken?.destinationId)
        assertNull(local.current)
        assertEquals(taken, repository.pendingRestore.value)

        repository.consumeRestore("home")
        assertEquals(taken, repository.pendingRestore.value)
        repository.consumeRestore("trips")
        assertNull(repository.pendingRestore.value)
    }

    @Test
    fun `T23 저장된 것이 없으면 복원도 없음`() = runTest {
        assertNull(repository.takeForRestore())
        assertNull(repository.pendingRestore.value)
    }
}
