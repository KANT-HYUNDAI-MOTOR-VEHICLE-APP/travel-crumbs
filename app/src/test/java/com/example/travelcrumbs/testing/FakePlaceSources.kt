package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.auth.AccessTokenSource
import com.example.travelcrumbs.data.pin.DefaultPinRepository
import com.example.travelcrumbs.data.pin.PinLocalDataSource
import com.example.travelcrumbs.data.pin.PinRepository
import com.example.travelcrumbs.data.pin.PinUploadScheduler
import com.example.travelcrumbs.data.pin.UploadRun
import com.example.travelcrumbs.data.place.DefaultPinnedPlacesRepository
import com.example.travelcrumbs.data.place.PinnedPlaceLocalDataSource
import com.example.travelcrumbs.data.place.PinnedPlacesRepository
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.place.PlaceStatsRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.NavigationStartIn
import com.example.travelcrumbs.data.remote.NavigationStartOut
import com.example.travelcrumbs.data.remote.NavigationStartRemoteDataSource
import com.example.travelcrumbs.data.remote.NavigationStatsOut
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.Pin
import com.example.travelcrumbs.domain.model.PinUploadState
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.PlaceStat
import com.example.travelcrumbs.domain.model.RecordingProgress
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** 시험용 후보 장소 — 이름 · 주소만 시험에 씀(좌표는 내비로만). */
fun testPlace(id: String, address: String = "$id 주소"): Place =
    Place(id = id, name = "$id 이름", address = address, latitude = 37.5, longitude = 127.0)

val TEST_PLACES = listOf("a", "b", "c", "d", "e").map { testPlace(it) }

class FakePlaceCatalogRepository(
    private val places: List<Place> = TEST_PLACES,
) : PlaceCatalogRepository {
    /** 후보 읽기를 늦출 때(T32) — 완료되기 전엔 후보를 내주지 않음. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun candidates(): List<Place> {
        gate?.await()
        return places
    }
}

/** 고정 목록 기기 저장 대역 — 같은 대역을 새 저장소에 넘기면 앱 재실행 흉내(T38). */
class FakePinnedPlaceLocalDataSource(
    initial: List<String> = emptyList(),
) : PinnedPlaceLocalDataSource {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    /** 늦게 읽힘(T32) — 거짓인 동안 흐름이 아무것도 내보내지 않음. */
    val readable = MutableStateFlow(true)

    override val pinnedIds: Flow<List<String>> =
        combine(state, readable) { ids, ok -> if (ok) ids else null }.filterNotNull()

    val current: List<String> get() = state.value

    var failWrites: Boolean = false
    var writes: Int = 0
        private set

    override suspend fun update(transform: (List<String>) -> List<String>) {
        if (failWrites) throw IOException("디스크 쓰기 실패(시험)")
        mutex.withLock {
            writes++
            state.value = transform(state.value)
        }
    }
}

/**
 * ui 시험용 고정 목록 묶음 — ui 패키지 시험이 DataSource 를 직접 들지 않게(구조 규칙 4) 기기 저장 대역을 안에 감춤.
 */
class FakePinnedPlacesStore(
    initial: List<String> = emptyList(),
    catalog: PlaceCatalogRepository,
) {
    private val local = FakePinnedPlaceLocalDataSource(initial)

    val repository: PinnedPlacesRepository = DefaultPinnedPlacesRepository(local, catalog)

    /** 기기 저장에 남은 고정 id. */
    val current: List<String> get() = local.current
    val writes: Int get() = local.writes

    /** 거짓인 동안 기기 저장을 아직 못 읽음(T32). */
    val readable: MutableStateFlow<Boolean> get() = local.readable

    var failWrites: Boolean
        get() = local.failWrites
        set(value) {
            local.failWrites = value
        }
}

fun pinnedPlacesRepositoryOf(
    local: PinnedPlaceLocalDataSource = FakePinnedPlaceLocalDataSource(),
    catalog: PlaceCatalogRepository = FakePlaceCatalogRepository(),
): PinnedPlacesRepository = DefaultPinnedPlacesRepository(local, catalog)

/** 핀 기기 저장 대역 — 같은 대역을 새 저장소에 넘기면 앱 재실행 흉내(T46 · T66). */
class FakePinLocalDataSource : PinLocalDataSource {
    private val state = MutableStateFlow<List<Pin>>(emptyList())

    override val pins: Flow<List<Pin>> = state

    val current: List<Pin> get() = state.value

    override suspend fun append(placeId: String, startedAtMillis: Long, utcOffsetSeconds: Int): Int {
        val next = (state.value.maxOfOrNull { it.sequence } ?: 0) + 1
        state.value = state.value + Pin(next, placeId, startedAtMillis, utcOffsetSeconds, PinUploadState.PENDING)
        return next
    }

    override suspend fun pending(): List<Pin> = state.value.filter { it.uploadState == PinUploadState.PENDING }

    override suspend fun setState(sequence: Int, state: PinUploadState) {
        this.state.value = this.state.value.map { if (it.sequence == sequence) it.copy(uploadState = state) else it }
    }
}

/** 서버 대역(AppApi) — 응답 코드 · 「받지 못함」(IOException)을 바꿔 가며 씀. */
class FakeAppApi(
    /** null 이면 네트워크 없음(IOException). */
    var postCode: Int? = 201,
) : AppApi {
    val posted = mutableListOf<NavigationStartIn>()
    var statsResponse: Response<NavigationStatsOut> = Response.success(NavigationStatsOut(emptyList()))
    var statsCalls: Int = 0
        private set

    override suspend fun createNavigationStart(body: NavigationStartIn): Response<NavigationStartOut> {
        val code = postCode ?: throw IOException("받지 못함(시험)")
        posted += body
        return if (code in 200..299) {
            Response.success(code, NavigationStartOut("id-${posted.size}", body.placeId, body.startedAt, body.startedAt))
        } else {
            Response.error(code, """{"code":"X"}""".toResponseBody())
        }
    }

    override suspend fun getNavigationStats(limit: Int): Response<NavigationStatsOut> {
        statsCalls++
        return statsResponse
    }
}

class FakeAccessTokenSource(var token: String? = "test-token") : AccessTokenSource {
    override fun accessToken(): String? = token
}

class FakePinUploadScheduler : PinUploadScheduler {
    /** 예약된 다시 보내기의 retryIndex 들. */
    val retries = mutableListOf<Int>()
    var appStarts: Int = 0
        private set

    override fun scheduleRetry(retryIndex: Int) {
        retries += retryIndex
    }

    override fun scheduleOnAppStart() {
        appStarts++
    }
}

/** 기록 상태 대역 — null 을 주면 「아직 못 읽음」(아무 값도 내보내지 않음 · T81). */
class FakeRecordingStatusRepository(initial: RecordingProgress?) : RecordingRepository {
    private val state = MutableStateFlow(initial)

    override val progress: Flow<RecordingProgress> = state.filterNotNull()

    fun set(value: RecordingProgress?) {
        state.value = value
    }

    override suspend fun togglePause() = Unit

    override suspend fun endTrip(): Long? = null

    override suspend fun startNewTrip() = Unit
}

fun pinRepositoryOf(
    local: PinLocalDataSource = FakePinLocalDataSource(),
    api: AppApi = FakeAppApi(),
    tokens: AccessTokenSource = FakeAccessTokenSource(),
    recording: RecordingRepository,
    time: TimeSource = FakeTimeSource(now = 1_700_000_000_000L),
    scheduler: PinUploadScheduler = FakePinUploadScheduler(),
): PinRepository = DefaultPinRepository(local, NavigationStartRemoteDataSource(api, tokens), recording, time, scheduler)

/** ViewModel 시험용 — 핀 저장소 호출만 셈(핀 규칙 자체는 PinRepositoryTest). */
class FakePinRepository : PinRepository {
    val recorded = mutableListOf<String>()
    private val state = MutableStateFlow<List<Pin>>(emptyList())

    override val pins: Flow<List<Pin>> = state

    override suspend fun recordGuidanceStart(placeId: String): Boolean {
        recorded += placeId
        state.value = state.value + Pin(state.value.size + 1, placeId, 0L, 0, PinUploadState.PENDING)
        return true
    }

    override suspend fun uploadPending(retryIndex: Int): UploadRun = UploadRun.DONE
}

class FakePlaceStatsRepository(
    var result: Result<List<PlaceStat>> = Result.success(emptyList()),
) : PlaceStatsRepository {
    override suspend fun topStats(limit: Int): Result<List<PlaceStat>> = result
}
