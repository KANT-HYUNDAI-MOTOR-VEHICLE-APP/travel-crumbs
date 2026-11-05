package com.example.travelcrumbs.data.draft

import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.Draft
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * 임시 저장 연결 지점(스펙 app-shell 4-1).
 *
 * 입력 칸이 있는 화면(#3 의 경험 입력 등)은
 * 1. 입력이 바뀔 때마다 [updateActiveInput] 으로 「지금 입력 중 내용」을 알리고(떠날 때 [clearActiveInput]),
 * 2. [pendingRestore] 에 자기 목적지의 값이 오면 칸을 채운 뒤 [consumeRestore] 로 지움.
 *
 * 앱 틀은 주행으로 바뀌는 순간 [saveActiveInput] · 「이어서 쓰기」 때 [takeForRestore] 를 부름.
 */
interface DraftRepository {
    /** 기기 안에 남은 임시 저장 — 있으면 「이어서 쓰기」 띠. */
    val savedDraft: Flow<Draft?>

    /** 복원할 내용 — 그 목적지 화면이 받아 쓰고 지움(상태로 전달 · 일회성 이벤트 아님). */
    val pendingRestore: StateFlow<Draft?>

    fun updateActiveInput(destinationId: String, fields: Map<String, String>)

    fun clearActiveInput(destinationId: String)

    /** 입력 중 내용이 있으면 기기 안에 저장. 없으면 아무 일도 안 함. */
    suspend fun saveActiveInput()

    /** 저장된 내용을 꺼내 [pendingRestore] 로 옮기고 저장을 비움. 없으면 null. */
    suspend fun takeForRestore(): Draft?

    fun consumeRestore(destinationId: String)
}

@Singleton
class DefaultDraftRepository @Inject constructor(
    private val local: DraftLocalDataSource,
    private val timeSource: TimeSource,
) : DraftRepository {

    private val activeInput = MutableStateFlow<ActiveInput?>(null)
    private val restore = MutableStateFlow<Draft?>(null)

    override val savedDraft: Flow<Draft?> = local.draft

    override val pendingRestore: StateFlow<Draft?> = restore.asStateFlow()

    override fun updateActiveInput(destinationId: String, fields: Map<String, String>) {
        activeInput.value = ActiveInput(destinationId, fields)
    }

    override fun clearActiveInput(destinationId: String) {
        if (activeInput.value?.destinationId == destinationId) activeInput.value = null
    }

    override suspend fun saveActiveInput() {
        // 첫 suspend 전에 내용을 떠 둠 — 입력 화면이 바로 사라져도 잃지 않게
        val input = activeInput.value ?: return
        if (input.fields.values.all { it.isBlank() }) return
        local.save(Draft(input.destinationId, input.fields, timeSource.nowMillis()))
    }

    override suspend fun takeForRestore(): Draft? {
        val draft = local.draft.first() ?: return null
        local.clear()
        restore.value = draft
        return draft
    }

    override fun consumeRestore(destinationId: String) {
        if (restore.value?.destinationId == destinationId) restore.value = null
    }

    private data class ActiveInput(
        val destinationId: String,
        val fields: Map<String, String>,
    )
}
