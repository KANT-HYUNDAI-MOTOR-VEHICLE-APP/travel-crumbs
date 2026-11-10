package com.example.travelcrumbs.data.vehicle

import com.example.travelcrumbs.domain.combineDrivingState
import com.example.travelcrumbs.domain.model.DrivingState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 화면 · ViewModel 이 차량 신호에 닿는 유일한 길 — 기어 + 주행 규제를 합성(스펙 app-shell 3-1). */
interface DrivingSignalRepository {
    val drivingState: Flow<DrivingState>
}

@Singleton
class DefaultDrivingSignalRepository @Inject constructor(
    gearDataSource: VehicleGearDataSource,
    uxRestrictionsDataSource: UxRestrictionsDataSource,
) : DrivingSignalRepository {

    /** 구독하는 화면이 여럿이어도 SDK · Car 콜백은 하나 — 마지막 구독이 끝나면 바로 해제하고 옛 값도 버림. */
    private val sharingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 기어 · 규제 값이 바뀔 때마다 하나씩 합성(#66 실측 M9) — `combine` 은 한꺼번에 온 값을 마지막 것만 남겨(얼림이 풀릴 때 쌓인 D → P → D 가 D 하나로)
     * 재출발 판정이 P 를 놓쳤음. 둘 다 첫 값이 온 뒤부터 내보냄(combine 과 같음).
     */
    override val drivingState: Flow<DrivingState> =
        combineEveryUpdate(
            gearDataSource.gearFlow(),
            uxRestrictionsDataSource.restrictionFlow(),
            ::combineDrivingState,
        )
            .distinctUntilChanged()
            .shareIn(
                scope = sharingScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                replay = 1,
            )
}

/**
 * [combine] 과 같되 값을 버리지 않음 — 두 흐름 어느 쪽이든 값이 올 때마다 (그때의 두 최신 값으로) 하나씩 내보냄.
 * 둘 다 첫 값이 오기 전엔 내보내지 않음.
 */
internal fun <A : Any, B : Any, R> combineEveryUpdate(a: Flow<A>, b: Flow<B>, transform: (A, B) -> R): Flow<R> = channelFlow {
    val lock = Mutex()
    var lastA: A? = null
    var lastB: B? = null
    launch {
        a.collect { value ->
            lock.withLock {
                lastA = value
                lastB?.let { send(transform(value, it)) }
            }
        }
    }
    launch {
        b.collect { value ->
            lock.withLock {
                lastB = value
                lastA?.let { send(transform(it, value)) }
            }
        }
    }
}
