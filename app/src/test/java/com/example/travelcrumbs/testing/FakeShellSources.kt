package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.recording.DefaultRecordingRepository
import com.example.travelcrumbs.data.recording.RecordingLocalDataSource
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.unfinishedinput.DefaultUnfinishedInputRepository
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputLocalDataSource
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.StartVoiceInputUseCase
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.UnfinishedInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 기기 안 저장 대역 — 앱 종료 뒤 복원(V22 · V37)은 같은 대역을 새 저장소에 다시 넘겨 흉내 냄. */
class FakeRecordingLocalDataSource(
    initial: CurrentTrip? = null,
) : RecordingLocalDataSource {

    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val trip: Flow<CurrentTrip?> = state

    val current: CurrentTrip? get() = state.value

    override suspend fun update(transform: (CurrentTrip?) -> CurrentTrip) {
        mutex.withLock { state.value = transform(state.value) }
    }
}

class FakeUnfinishedInputLocalDataSource(
    initial: UnfinishedInput? = null,
) : UnfinishedInputLocalDataSource {

    private val state = MutableStateFlow(initial)

    override val unfinishedInput: Flow<UnfinishedInput?> = state

    val current: UnfinishedInput? get() = state.value

    override suspend fun save(input: UnfinishedInput) {
        state.value = input
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

fun unfinishedInputRepositoryOf(
    timeSource: TimeSource,
    local: UnfinishedInputLocalDataSource = FakeUnfinishedInputLocalDataSource(),
): UnfinishedInputRepository = DefaultUnfinishedInputRepository(local, timeSource)
