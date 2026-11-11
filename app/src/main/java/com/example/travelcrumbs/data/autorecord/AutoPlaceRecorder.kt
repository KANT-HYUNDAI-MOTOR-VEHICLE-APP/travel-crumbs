package com.example.travelcrumbs.data.autorecord

import android.util.Log
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.trip.placeNameFromAddr
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.AutoRecordRules
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.PlaceRecordCleanup
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.AutoPlace
import com.example.travelcrumbs.domain.model.AutoPlaceName
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.DraftPlaceInput
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.NaviEvent
import com.example.travelcrumbs.domain.model.PendingArrival
import com.example.travelcrumbs.domain.model.TripStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 내비 목적지 자동 장소 기록(#66) — 앱 수명 하나(di 에서 Singleton · 스펙 3.8 「재출발 판정의 주인」).
 *
 * - 내비 상시 알림(시작 · 도착 · 취소)과 주행 판정(PARKED · DRIVING · UNKNOWN)을 SDK 세션 동안 구독([attach] · [detach]).
 * - 직접 시작한 안내의 목적지를 시작 알림 때 조회해 메모리에 기억 → 이름이 같은 도착 알림 → 기기 안 기록 대기 →
 *   도착 뒤 PARKED 를 거친 첫 DRIVING 때 `trip_place_dtl` 1행으로 확정(3.3 · 3.4). 판정은 [AutoRecordRules](순수 계산).
 * - 사건(알림 · 주행 판정 변화 · 수동 기록 · 여행 종료 · 철회 · 삭제)은 [mutex] 로 한 번에 하나씩 끝까지(3.8 「차례 처리」).
 *   단 시작 알림의 기다림(1.5초) · 목적지 조회(SDK · 최대 10초)는 [mutex] 밖에서 — 종료 · 철회 · 주행 판정이 그동안 막히지 않음.
 *   내비 알림은 한 구독에서 차례로 받으므로 시작 처리가 끝나기 전에 온 도착은 그 뒤에 처리(T46). 조회 사이 철회 · 모두 지우기 · 새 시작이
 *   끼면 [guideGeneration] 이 바뀌어 늦은 조회 결과는 버림.
 * - 로그엔 사건 종류 · 처리 결과 코드 · 건수만 — 이름 · 주소 · 좌표 · 도착 시각 · 장소 번호 없음(4.3 · T34).
 */
class AutoPlaceRecorder(
    private val navi: NaviRepository,
    private val driving: DrivingSignalRepository,
    private val recording: RecordingRepository,
    private val users: CurrentUserRepository,
    private val trips: TripRepository,
    private val unfinishedInputs: UnfinishedInputRepository,
    private val guideRequests: GuideRequestTracker,
    private val store: AutoRecordLocalDataSource,
    private val timeSource: TimeSource,
    private val scope: CoroutineScope,
) : PlaceRecordCleanup {

    private val mutex = Mutex()

    /** 진행 중 직접 안내(메모리 · 3.1) — 프로세스가 끝나면 사라짐(N15). [mutex] 안에서만 읽고 씀. */
    private var directGuide: NaviDestination? = null

    /** 진행 중 직접 안내 세대 — 시작 알림 · 철회 · 모두 지우기마다 늘어남. 조회가 끝났을 때 세대가 다르면 그 결과는 버림. [mutex] 안에서만. */
    private var guideGeneration = 0L

    private var session: Job? = null
    private var appStartDone = false

    /** SDK 세션 시작(PleosSdkSession) — 알림 · 주행 판정 구독. 이미 붙어 있으면 그대로. 앱 새로 시작 처리는 프로세스마다 한 번. */
    fun attach() {
        if (session?.isActive == true) return
        session = scope.launch {
            launch {
                mutex.withLock {
                    if (!appStartDone) {
                        appStartDone = true
                        guarded("app_start") { onAppStart() }
                    }
                }
            }
            launch {
                navi.events.collect { event ->
                    if (event == NaviEvent.RouteStarted) {
                        onRouteStarted()
                    } else {
                        mutex.withLock { guarded("navi_event") { onNaviEvent(event) } }
                    }
                }
            }
            // 처리가 늦어도(차례 대기) 공유 주행 판정 흐름 · 다른 구독자를 막지 않게 받은 값은 따로 쌓아 둠(값은 드묾 · 버리지 않음)
            launch {
                driving.drivingStateChanges
                    .buffer(Channel.UNLIMITED)
                    .collect { state -> mutex.withLock { guarded("driving") { onDriving(state) } } }
            }
        }
    }

    /** SDK 세션 끝 — 구독을 거둠(대기는 기기 안에 남음). */
    fun detach() {
        session?.cancel()
        session = null
    }

    /**
     * 수동 「이 장소 기록」(3.4 「수동」 행 · N9) — 수동 행을 넣기 직전에 그 여행의 대기를 정리(반경 안 = 폐기 · 나머지 = 먼저 확정)한 뒤
     * [insertManual] 을 같은 차례 안에서 부름. 여행이 PAUSED 면 대기는 그대로.
     */
    suspend fun <T> withManualRecord(tripNo: Long, lat: Double?, lng: Double?, insertManual: suspend () -> T): T =
        mutex.withLock {
            guarded("manual") {
                val ctx = context()
                if (ctx != null && ctx.tripNo == tripNo && ctx.status == TripStatus.RECORDING && ctx.agreed) {
                    val all = AutoRecordRules.purge(store.pendings(), timeSource.nowMillis(), applyParkWait = true)
                    val mine = all.filter { it.tripNo == tripNo }
                    if (mine.isNotEmpty()) {
                        val candidates = AutoRecordRules.beforeManual(mine, lat, lng)
                        Log.i(TAG, "수동 기록 전 정리 · 폐기 ${mine.size - candidates.size}건")
                        confirm(candidates, tripNo, fillUnfinished = false)
                    }
                    store.savePendings(all - mine.toSet())
                }
            }
            insertManual()
        }

    /** 「여행 종료」(3.4 · N3 결정) — 종료 직전에 그 여행의 대기를 PARKED 여부 무관 확정 → [endTrip] 을 같은 차례 안에서 부름. */
    suspend fun <T> withEndTrip(endTrip: suspend () -> T): T =
        mutex.withLock {
            guarded("end_trip") {
                val ctx = context()
                val tripNo = ctx?.tripNo
                if (ctx != null && tripNo != null && ctx.status.inProgress && ctx.agreed) {
                    val all = AutoRecordRules.purge(store.pendings(), timeSource.nowMillis(), applyParkWait = true)
                    val mine = all.filter { it.tripNo == tripNo }
                    if (mine.isNotEmpty()) confirm(mine, tripNo, fillUnfinished = true)
                    store.savePendings(all - mine.toSet())
                }
            }
            endTrip()
        }

    override suspend fun withdrawLocation() {
        mutex.withLock {
            guideGeneration++
            directGuide = null
            // 대기 · 원본 삭제가 늘 먼저 — 장소 표 정리가 실패해도 기기 안 도착 좌표 · 주소가 남지 않게(심사 개인정보)
            val names = try {
                store.names().associate { it.tripPlaceNo to it.name }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Log.w(TAG, "철회 · 원본 읽기 실패: ${error.javaClass.simpleName}")
                emptyMap()
            }
            store.clearAll()
            Log.i(TAG, "위치 동의 철회 · 대기 · 원본 삭제")
            trips.clearLocations(names)
        }
    }

    override suspend fun deleteTrip(tripNo: Long) {
        mutex.withLock {
            val placeNos = trips.trip(tripNo).first()?.places?.map { it.tripPlaceNo }.orEmpty().toSet()
            trips.deleteTrip(tripNo)
            store.savePendings(store.pendings().filterNot { it.tripNo == tripNo })
            store.saveNames(store.names().filterNot { it.tripPlaceNo in placeNos })
        }
    }

    override suspend fun deleteAll() {
        mutex.withLock {
            guideGeneration++
            directGuide = null
            trips.deleteAll()
            store.clearAll()
        }
    }

    /** 앱 새로 시작(3.4) — 24시간 지난 대기를 먼저 지우고 남은 것은 「PARKED 봄」(시동이 꺼졌다 켜졌으면 차가 섰던 것 · N2). */
    private suspend fun onAppStart() {
        val before = store.pendings()
        if (before.isEmpty()) return
        val after = AutoRecordRules.markParked(AutoRecordRules.purge(before, timeSource.nowMillis(), applyParkWait = false))
        store.savePendings(after)
        Log.i(TAG, "앱 시작 · 대기 ${after.size}건 · 만료 ${before.size - after.size}건")
    }

    private suspend fun onNaviEvent(event: NaviEvent) {
        when (event) {
            NaviEvent.RouteStarted -> Unit // [onRouteStarted] 가 따로(조회를 차례 밖에서)
            is NaviEvent.DestinationArrived -> onArrived(event.name)
            NaviEvent.RouteCancelled -> {
                directGuide = null
                Log.i(TAG, "취소 · 진행 중 직접 안내 비움")
            }
        }
    }

    /**
     * 시작 알림(3.3) — 조건이 맞을 때만 목적지 조회 · 이 앱 요청이면 기억하지 않음.
     * 조건 확인 · 결과 반영은 차례([mutex]) 안 · 기다림 · 조회는 밖(종료 · 철회 · 주행 판정이 막히지 않게).
     */
    private suspend fun onRouteStarted() {
        val generation = mutex.withLock {
            var started: Long? = null
            guarded("route_start") {
                val generation = ++guideGeneration
                directGuide = null
                if (startable(context())) {
                    started = generation
                } else {
                    Log.i(TAG, "시작 · 건너뜀 NOT_RECORDING_OR_NO_CONSENT")
                }
            }
            started
        } ?: return
        // 얼림이 풀린 직후 쌓인 콜백이 먼저 지나가게 — 그 사이 조회는 옛 상태를 받을 수 있음(기존 규칙 F9)
        delay(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS)
        val destination = try {
            navi.guidedDestination()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w(TAG, "시작 · 조회 실패: ${error.javaClass.simpleName}")
            null
        }
        mutex.withLock { guarded("route_start") { applyStart(generation, destination) } }
    }

    private fun startable(ctx: Context?): Boolean =
        ctx != null && ctx.status == TripStatus.RECORDING && ctx.agreed && navi.ready.value

    /** 조회 결과 반영 — 그 사이 세대가 바뀌었거나(철회 · 모두 지우기) 기록 · 동의가 끝났으면 버림. */
    private suspend fun applyStart(generation: Long, destination: NaviDestination?) {
        if (generation != guideGeneration) {
            Log.i(TAG, "시작 · 결과 버림 STALE")
            return
        }
        val ctx = context()
        if (ctx == null || ctx.status != TripStatus.RECORDING || !ctx.agreed) {
            directGuide = null
            Log.i(TAG, "시작 · 결과 버림 NOT_RECORDING_OR_NO_CONSENT")
            return
        }
        if (destination == null || !AutoRecordRules.usable(destination)) {
            directGuide = null
            Log.i(TAG, "시작 · 건너뜀 DESTINATION_UNKNOWN")
            return
        }
        if (guideRequests.isAppRequest(destination)) {
            directGuide = null
            Log.i(TAG, "시작 · 건너뜀 APP_REQUEST")
            return
        }
        directGuide = destination
        val before = store.pendings()
        val after = AutoRecordRules.dropPassed(AutoRecordRules.purge(before, timeSource.nowMillis(), applyParkWait = true), destination)
        if (after != before) store.savePendings(after)
        Log.i(TAG, "시작 · 직접 안내 기억 · 지나친 대기 폐기 ${before.size - after.size}건")
    }

    /** 도착 알림(3.3) — 이름이 진행 중 직접 안내와 같으면 기록 대기 1건. 조회하지 않음(좌표는 시작 때 얻은 값). */
    private suspend fun onArrived(arrivedName: String) {
        val guide = directGuide
        if (guide == null) {
            Log.i(TAG, "도착 · 건너뜀 NO_DIRECT_GUIDE")
            return
        }
        val ctx = context()
        val tripNo = ctx?.tripNo
        if (ctx == null || tripNo == null || ctx.status != TripStatus.RECORDING || !ctx.agreed) {
            directGuide = null
            Log.i(TAG, "도착 · 건너뜀 NOT_RECORDING_OR_NO_CONSENT")
            return
        }
        if (!AutoRecordRules.sameName(arrivedName, guide.name)) {
            Log.i(TAG, "도착 · 건너뜀 NAME_MISMATCH")
            return
        }
        val now = timeSource.nowMillis()
        val arrival = PendingArrival(
            tripNo = tripNo,
            name = guide.name,
            address = guide.address,
            latitude = guide.latitude,
            longitude = guide.longitude,
            arrivedAt = now,
            parkedSeen = false,
        )
        val after = AutoRecordRules.add(AutoRecordRules.purge(store.pendings(), now, applyParkWait = true), arrival)
        store.savePendings(after)
        directGuide = null
        Log.i(TAG, "도착 · 대기 추가 · 대기 ${after.size}건")
    }

    /** 주행 판정 변화(3.4) — PARKED = 「PARKED 봄」 표시 · DRIVING = 「PARKED 봄」 대기 확정 · UNKNOWN = 아무것도 안 함(N2). */
    private suspend fun onDriving(state: DrivingState) {
        Log.d(TAG, DRIVING_LOG.getValue(state))
        if (state == DrivingState.UNKNOWN) return
        val before = store.pendings()
        if (before.isEmpty()) return
        var list = AutoRecordRules.purge(before, timeSource.nowMillis(), applyParkWait = true)
        when (state) {
            DrivingState.PARKED -> list = AutoRecordRules.markParked(list)
            DrivingState.DRIVING -> list = onRestart(list)
            DrivingState.UNKNOWN -> Unit
        }
        if (list != before) store.savePendings(list)
    }

    /** 재출발 — 「PARKED 봄」 대기를 그 여행이 RECORDING · 동의함이면 확정 · PAUSED 면 유지 · 여행이 끝났거나 바뀌었으면 폐기. */
    private suspend fun onRestart(list: List<PendingArrival>): List<PendingArrival> {
        if (list.none { it.parkedSeen }) return list
        val ctx = context() ?: return list // 못 읽음 — 다음 기회로
        if (!ctx.agreed) {
            // 동의 없음(철회 · 거부) — 위치 정보인 대기를 남기지 않음(철회 처리가 이미 지웠어도 한 번 더)
            Log.i(TAG, "재출발 · 동의 없음 대기 폐기 ${list.size}건")
            return emptyList()
        }
        val tripNo = ctx.tripNo
        // 지금 진행 중 여행이 아닌 여행의 대기(끝남 · 다른 여행 시작 · 여행 없음) = 폐기(3.4)
        val alive = list.filter { tripNo != null && it.tripNo == tripNo && ctx.status.inProgress }
        if (alive.size != list.size) Log.i(TAG, "재출발 · 다른 여행 대기 폐기 ${list.size - alive.size}건")
        if (tripNo == null || ctx.status != TripStatus.RECORDING) return alive
        val ready = alive.filter { it.parkedSeen }
        confirm(ready, tripNo, fillUnfinished = true)
        return alive - ready.toSet()
    }

    /**
     * 확정 판정(3.4 표) — 현재 여행 장소 · 앞선 확정과 반경 안이면 빼고 1행씩 기록 → 자동 이름 원본 → 「이어서 쓰기」 칸(비었을 때 · 가장 이른 1건).
     */
    private suspend fun confirm(candidates: List<PendingArrival>, tripNo: Long, fillUnfinished: Boolean) {
        if (candidates.isEmpty()) return
        val existing = trips.trip(tripNo).first()?.places.orEmpty().mapNotNull { place ->
            val lat = place.lat
            val lng = place.lng
            if (lat != null && lng != null) lat to lng else null
        }
        val toRecord = AutoRecordRules.toRecord(candidates, existing)
        val names = store.names().toMutableList()
        var unfinishedChecked = !fillUnfinished
        toRecord.forEach { pending ->
            val name = autoPlaceName(pending.name, pending.address)
            val tripPlaceNo = trips.addAutoPlace(
                tripNo,
                AutoPlace(
                    userPlaceNm = name,
                    addr = pending.address?.trim()?.takeIf { it.isNotEmpty() },
                    lat = pending.latitude,
                    lng = pending.longitude,
                    recordedAt = pending.arrivedAt,
                ),
            )
            if (name != null) names += AutoPlaceName(tripPlaceNo, name)
            if (!unfinishedChecked) {
                unfinishedChecked = true
                // 칸이 비어 있을 때만(작성 중 내용을 덮어쓰지 않음 · N6) · 칸엔 장소 번호만(F26)
                if (unfinishedInputs.savedInput.first() == null) {
                    unfinishedInputs.saveNow(
                        DraftPlaceInput.DESTINATION_ID,
                        mapOf(DraftPlaceInput.FIELD_TRIP_PLACE_NO to tripPlaceNo.toString()),
                    )
                }
            }
        }
        store.saveNames(names)
        Log.i(TAG, "확정 ${toRecord.size}건 · 중복 ${candidates.size - toRecord.size}건")
    }

    /** 기록 상태 · 동의 한 번 읽기 — 한도 안에 못 읽으면 null(못 읽음). */
    private suspend fun context(): Context? = withTimeoutOrNull(AppConfig.RECORDING_STATUS_READ_TIMEOUT_MILLIS) {
        val progress = recording.progress.first()
        val agreed = users.consents.first()[ConsentType.LOCATION_COLLECT]?.agreed == true
        Context(progress.tripNo, progress.status, agreed)
    }

    /** 처리 하나의 실패가 구독 · 차례를 끊지 않게 — 로그엔 예외 종류만. */
    private suspend fun guarded(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w(TAG, "$what 처리 실패: ${error.javaClass.simpleName}")
        }
    }

    private data class Context(val tripNo: Long?, val status: TripStatus, val agreed: Boolean)

    private companion object {
        const val TAG = "AutoPlaceRecorder"

        /** 주행 판정 받음 로그(사건 종류만 · 실측 M9 확인용). */
        val DRIVING_LOG = mapOf(
            DrivingState.PARKED to "주행 판정 PARKED",
            DrivingState.DRIVING to "주행 판정 DRIVING",
            DrivingState.UNKNOWN to "주행 판정 UNKNOWN",
        )
    }
}

/** 확정 때 넣을 이름(4.1) — 내비 목적지 이름(앞뒤 공백 제거 · 길이 한도) · 비었으면 주소에서 만든 처음 값 · 둘 다 없으면 null. */
internal fun autoPlaceName(name: String?, address: String?): String? =
    name?.trim()?.take(AppConfig.PLACE_NAME_MAX_LENGTH)?.takeIf { it.isNotEmpty() }
        ?: address?.trim()?.takeIf { it.isNotEmpty() }?.let(::placeNameFromAddr)
