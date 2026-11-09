package com.example.travelcrumbs.data.unfinishedinput

import com.example.travelcrumbs.domain.model.UnfinishedInput
import com.example.travelcrumbs.testing.FakeUnfinishedInputLocalDataSource
import com.example.travelcrumbs.testing.FakeTimeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnfinishedInputRepositoryTest {

    private val local = FakeUnfinishedInputLocalDataSource()
    private val repository = DefaultUnfinishedInputRepository(local, FakeTimeSource(now = 42L))

    @Test
    fun `T21 V27 입력 중 내용이 있으면 저장`() = runTest {
        repository.updateActiveInput("trips", mapOf("memo" to "바다"))

        repository.saveActiveInput()

        assertEquals(UnfinishedInput("trips", mapOf("memo" to "바다"), 42L), repository.savedInput.first())
    }

    @Test
    fun `T24 V27 입력 중 내용이 없거나 비어 있으면 저장 안 함`() = runTest {
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
    fun `T23 V27 이어서 쓰기 — 복원 상태로 옮기고 저장은 비움 · 받은 화면이 지움`() = runTest {
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
    fun `T23 V27 저장된 것이 없으면 복원도 없음`() = runTest {
        assertNull(repository.takeForRestore())
        assertNull(repository.pendingRestore.value)
    }
}
