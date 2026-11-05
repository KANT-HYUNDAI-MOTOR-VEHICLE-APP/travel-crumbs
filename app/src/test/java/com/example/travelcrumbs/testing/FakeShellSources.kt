package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.draft.DefaultDraftRepository
import com.example.travelcrumbs.data.draft.DraftLocalDataSource
import com.example.travelcrumbs.data.draft.DraftRepository
import com.example.travelcrumbs.data.recording.DefaultRecordingRepository
import com.example.travelcrumbs.data.recording.RecordingLocalDataSource
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.StartVoiceInputUseCase
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.Draft
import com.example.travelcrumbs.domain.model.RecordingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 기기 안 저장 대역 — 앱 종료 뒤 복원(T33)은 같은 대역을 새 저장소에 다시 넘겨 흉내 냄. */
class FakeRecordingLocalDataSource(
    initial: RecordingSession? = null,
) : RecordingLocalDataSource {

    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val session: Flow<RecordingSession?> = state

    val current: RecordingSession? get() = state.value

    override suspend fun update(transform: (RecordingSession?) -> RecordingSession) {
        mutex.withLock { state.value = transform(state.value) }
    }
}

class FakeDraftLocalDataSource(
    initial: Draft? = null,
) : DraftLocalDataSource {

    private val state = MutableStateFlow(initial)

    override val draft: Flow<Draft?> = state

    val current: Draft? get() = state.value

    override suspend fun save(draft: Draft) {
        state.value = draft
    }

    override suspend fun clear() {
        state.value = null
    }
}

/** 손으로 돌리는 시계. */
class FakeTimeSource(var now: Long = 0L) : TimeSource {
    override fun nowMillis(): Long = now
}

class FakeRecordPlaceUseCase(private val result: ActionResult = ActionResult.NOT_READY) : RecordPlaceUseCase {
    var calls: Int = 0
        private set

    override suspend fun invoke(): ActionResult {
        calls++
        return result
    }
}

class FakeStartVoiceInputUseCase(private val result: ActionResult = ActionResult.NOT_READY) : StartVoiceInputUseCase {
    override suspend fun invoke(): ActionResult = result
}

/** ui 패키지의 테스트가 DataSource 를 직접 들지 않게(구조 규칙 4) 저장소를 여기서 조립. */
fun recordingRepositoryOf(
    timeSource: TimeSource,
    local: RecordingLocalDataSource = FakeRecordingLocalDataSource(),
): RecordingRepository = DefaultRecordingRepository(local, timeSource)

fun draftRepositoryOf(
    timeSource: TimeSource,
    local: DraftLocalDataSource = FakeDraftLocalDataSource(),
): DraftRepository = DefaultDraftRepository(local, timeSource)
