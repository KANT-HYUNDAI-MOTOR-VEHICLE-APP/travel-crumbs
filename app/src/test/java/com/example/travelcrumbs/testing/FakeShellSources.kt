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

    /** null 이면 그대로(#51 U1 — 여행이 없는데 일시정지 · 종료). 번호는 붙이지 않음(Room 판은 trip_no 를 붙임). */
    override suspend fun update(transform: (CurrentTrip?) -> CurrentTrip?) {
        mutex.withLock { transform(state.value)?.let { state.value = it } }
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
/** ui 패키지 시험이 자료원 이름을 import 하지 않게(Konsist 규칙 4) — 기기 안 저장 대역 하나. */
fun fakeRecordingLocal(initial: CurrentTrip? = null) = FakeRecordingLocalDataSource(initial)

fun recordingRepositoryOf(
    timeSource: TimeSource,
    /** 기본 = 지금 시각에 시작한 기록 중 여행 하나(v6 는 여행을 자동으로 만들지 않음 — 옛 시험의 「첫 실행」 자리). */
    local: RecordingLocalDataSource = FakeRecordingLocalDataSource(CurrentTrip.start(timeSource.nowMillis())),
): RecordingRepository = DefaultRecordingRepository(local, timeSource)

fun unfinishedInputRepositoryOf(
    timeSource: TimeSource,
    local: UnfinishedInputLocalDataSource = FakeUnfinishedInputLocalDataSource(),
): UnfinishedInputRepository = DefaultUnfinishedInputRepository(local, timeSource)

/** 진행 중 여행이 없는 기록 저장소(v6 첫 화면 · #55 홈 근처 로드맵 시험) — 화면 시험이 자료원 대역을 직접 들지 않게. */
fun idleRecordingRepositoryOf(timeSource: TimeSource): RecordingRepository =
    recordingRepositoryOf(timeSource, FakeRecordingLocalDataSource())
