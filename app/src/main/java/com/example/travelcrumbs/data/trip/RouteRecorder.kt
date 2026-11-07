package com.example.travelcrumbs.data.trip

import android.util.Log
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RouteRules
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.RoutePoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 경로 기록(A-05) — 기록 중인 여행이 있는 동안 내비에 현재 위치를 묻고(RouteRules.POLL_MILLIS),
 * 움직였을 때만 경로점을 모아 서버(trip_route_point_dtl)로 보냄.
 * 앱이 켜져 있는 동안만 기록(백그라운드 · 앱 종료 뒤 기록은 시나리오 1 범위 밖).
 * MainActivity 가 start() · stop() 을 부름.
 */
@Singleton
class RouteRecorder @Inject constructor(
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val timeSource: TimeSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // 앱을 다시 켰을 때 기록 중이던 여행을 이어서 기록
            tripRepository.currentTrip()
            tripRepository.activeTrip
                .map { it?.tripUuid }
                .distinctUntilChanged()
                .collectLatest { tripUuid -> if (tripUuid != null) record(tripUuid) }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun record(tripUuid: String) {
        val pending = ArrayDeque<RoutePoint>()
        var last: RoutePoint? = null
        var lastFlush = timeSource.nowMillis()
        try {
            while (currentCoroutineContext().isActive) {
                val location = if (naviRepository.ready.value) {
                    runCatching { withTimeout(AppConfig.SDK_CALL_TIMEOUT_MS) { naviRepository.currentLocation() } }
                        .onFailure { Log.d(TAG, "위치 조회 실패: ${it.message}") }
                        .getOrNull()
                } else {
                    null
                }
                if (location != null && location.isValid) {
                    val point = RoutePoint(location.lat, location.lng, timeSource.nowMillis())
                    if (RouteRules.shouldKeep(last, point)) {
                        pending.addLast(point)
                        last = point
                        while (pending.size > RouteRules.MAX_PENDING) pending.removeFirst()
                    }
                }
                val now = timeSource.nowMillis()
                if (pending.size >= RouteRules.BATCH_SIZE ||
                    (pending.isNotEmpty() && now - lastFlush >= RouteRules.FLUSH_MILLIS)
                ) {
                    flush(tripUuid, pending)
                    lastFlush = now
                }
                delay(RouteRules.POLL_MILLIS)
            }
        } finally {
            // 여행이 바뀌거나 앱이 닫힐 때 남은 점을 한 번 더 보냄
            withContext(NonCancellable) { flush(tripUuid, pending) }
        }
    }

    private suspend fun flush(tripUuid: String, pending: ArrayDeque<RoutePoint>) {
        if (pending.isEmpty()) return
        val batch = pending.toList()
        tripRepository.addRoutePoints(tripUuid, batch)
            .onSuccess { repeat(batch.size) { pending.removeFirst() } }
            .onFailure { Log.d(TAG, "경로점 전송 실패(다음에 다시 보냄): ${it.message}") }
    }

    private companion object {
        const val TAG = "RouteRecorder"
    }
}
