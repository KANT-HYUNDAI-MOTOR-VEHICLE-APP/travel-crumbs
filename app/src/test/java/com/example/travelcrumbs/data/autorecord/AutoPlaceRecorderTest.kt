package com.example.travelcrumbs.data.autorecord

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.model.AutoPlaceName
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.DraftPlaceInput
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.NaviEvent
import com.example.travelcrumbs.domain.model.PendingArrival
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.testing.FakeAutoRecordLocalDataSource
import com.example.travelcrumbs.testing.FakeCurrentUserRepository
import com.example.travelcrumbs.testing.FakeDrivingSignalRepository
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakePinRepository
import com.example.travelcrumbs.testing.FakeRecordingLocalDataSource
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import com.example.travelcrumbs.testing.FakeUnfinishedInputLocalDataSource
import com.example.travelcrumbs.testing.recordingRepositoryOf
import com.example.travelcrumbs.testing.unfinishedInputRepositoryOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 내비 목적지 자동 장소 기록(#66) 단위 시험 — fake 내비(알림 · 조회) · fake 주행 판정 · fake 기록 상태 · fake 동의 · fake 시계 · 기기 저장 fake.
 * 시험 이름의 T 번호 = 스펙 6절.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoPlaceRecorderTest {

    private class Fixture(
        val scope: TestScope,
        val store: FakeAutoRecordLocalDataSource = FakeAutoRecordLocalDataSource(),
        val time: FakeTimeSource = FakeTimeSource(now = START),
    ) {
        val navi = FakeNaviRepository()
        val driving = FakeDrivingSignalRepository(DrivingState.UNKNOWN)
        val recordingLocal = FakeRecordingLocalDataSource(CurrentTrip.start(time.now).copy(tripNo = TRIP))
        val recording = recordingRepositoryOf(time, recordingLocal)
        val users = FakeCurrentUserRepository(time = time)
        val trips = FakeTripRepository(listOf(trip(TRIP)), time)
        val inputLocal = FakeUnfinishedInputLocalDataSource()
        val inputs = unfinishedInputRepositoryOf(time, inputLocal)
        val pins = FakePinRepository()
        var guide = GuideRequestTracker(navi, pins, scope.backgroundScope, time)
        var recorder = create()

        fun create() = AutoPlaceRecorder(navi, driving, recording, users, trips, inputs, guide, store, time, scope.backgroundScope)

        /** 프로세스 새로 시작 흉내 — 메모리(진행 중 직접 안내 · 요청 입구 기억)는 사라지고 기기 저장은 남음. */
        fun restartProcess() {
            recorder.detach()
            guide = GuideRequestTracker(navi, pins, scope.backgroundScope, time)
            recorder = create()
            recorder.attach()
            scope.runCurrent()
        }

        val places get() = trips.current.single { it.tripNo == TRIP }.places
    }

    private suspend fun TestScope.ready(f: Fixture = Fixture(this), consent: Boolean = true): Fixture {
        if (consent) f.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        f.recorder.attach()
        runCurrent()
        return f
    }

    private suspend fun TestScope.start(f: Fixture, destination: NaviDestination?) {
        f.navi.destination = destination
        f.navi.naviEvents.emit(NaviEvent.RouteStarted)
        advanceTimeBy(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 1)
        runCurrent()
    }

    private suspend fun TestScope.arrive(f: Fixture, name: String) {
        f.navi.naviEvents.emit(NaviEvent.DestinationArrived(name))
        runCurrent()
    }

    private fun TestScope.drive(f: Fixture, vararg states: DrivingState) {
        states.forEach {
            f.driving.emit(it)
            runCurrent()
        }
    }

    @Test
    fun `T1 직접 시작 알림 — 목적지 조회 1번 · 장소 0 · 대기 0`() = runTest {
        val f = ready()
        start(f, A)
        assertEquals(1, f.navi.destinationQueries)
        assertTrue(f.places.isEmpty())
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T2 T3 도착 → 대기 1 · P → D 때 장소 1행(draft · 순서 1 · 기록 시각 = 도착 시각) · 원본 1 · 띠 칸 = 장소 번호`() = runTest {
        val f = ready()
        start(f, A)
        f.time.now += 60_000
        val arrivedAt = f.time.now
        arrive(f, " 경복궁 ")
        val pending = f.store.pendingList.single()
        assertEquals(PendingArrival(TRIP, A.name, A.address, A.latitude, A.longitude, arrivedAt, parkedSeen = false), pending)
        assertTrue(f.places.isEmpty())

        f.time.now += 120_000
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)

        val place = f.places.single()
        assertEquals("경복궁", place.userPlaceNm)
        assertEquals(A.address, place.addr)
        assertEquals(A.latitude, place.lat!!, 0.0)
        assertTrue(place.draft)
        assertEquals(1, place.placeOrd)
        assertEquals(arrivedAt, place.recordedAt)
        assertTrue(f.store.pendingList.isEmpty())
        assertEquals(listOf(AutoPlaceName(place.tripPlaceNo, "경복궁")), f.store.nameList)
        val saved = f.inputLocal.unfinishedInput.first()!!
        assertEquals(DraftPlaceInput.DESTINATION_ID, saved.destinationId)
        assertEquals(mapOf(DraftPlaceInput.FIELD_TRIP_PLACE_NO to place.tripPlaceNo.toString()), saved.fields)
    }

    @Test
    fun `T4 도착 뒤 DRIVING 만 계속 — 장소 0 · 대기 유지`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.DRIVING)
        assertTrue(f.places.isEmpty())
        assertEquals(1, f.store.pendingList.size)
    }

    @Test
    fun `T5 도착 뒤 10분 동안 PARKED 없음 — 대기 폐기 · 장소 0`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        f.time.now += AppConfig.AUTO_RECORD_PARK_WAIT_MILLIS
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T6 PARKED → UNKNOWN — 재출발 아님 · 대기 유지(PARKED 봄)`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.UNKNOWN)
        assertTrue(f.places.isEmpty())
        assertTrue(f.store.pendingList.single().parkedSeen)
    }

    @Test
    fun `T7 가는 길에 수동 B → A 도착 → P → D — 장소 B(1) · A(2)`() = runTest {
        val f = ready()
        start(f, A)
        f.trips.addPlace(TRIP, CurrentLocation(A.latitude + KM, A.longitude, "서울 종로구 세종대로 1"), draft = true)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf("서울 종로구 세종대로 1", "경복궁"), f.places.sortedBy { it.placeOrd }.map { it.userPlaceNm })
        assertEquals(listOf(1, 2), f.places.sortedBy { it.placeOrd }.map { it.placeOrd })
    }

    @Test
    fun `T8 대기 A · P 뒤 A 에서 수동 기록(50 m) → D — 대기 폐기 · 수동 1행만`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED)
        val manual = CurrentLocation(A.latitude + M50, A.longitude, "서울 종로구 사직로 161")
        f.recorder.withManualRecord(TRIP, manual.lat, manual.lng) { f.trips.addPlace(TRIP, manual, draft = true) }
        drive(f, DrivingState.DRIVING)
        assertEquals(1, f.places.size)
        assertTrue(f.trips.autoPlaceNos.isEmpty())
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T9 진행 중 A 취소 → 직접 C 시작 → C 도착 → P → D — 장소 C 만`() = runTest {
        val f = ready()
        start(f, A)
        f.navi.naviEvents.emit(NaviEvent.RouteCancelled)
        runCurrent()
        arrive(f, A.name)
        assertTrue("취소한 A 의 도착은 기록 안 함", f.store.pendingList.isEmpty())
        start(f, C)
        arrive(f, C.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf("남산서울타워"), f.places.map { it.userPlaceNm })
    }

    @Test
    fun `T10 T19 시작을 아는 프로세스가 끝남 → 새 프로세스에서 도착 — 대기 0 · 장소 0`() = runTest {
        val f = ready()
        start(f, A)
        f.restartProcess()
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T11 대기 A(PARKED 아직) · 다른 C(1 km 넘게) 시작 — 대기 A 폐기(지나침)`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        start(f, C)
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T12 저장된 대기 A(PARKED 아직 · 1시간 전) · 프로세스 새로 시작 → DRIVING — 장소 A`() = runTest {
        val store = FakeAutoRecordLocalDataSource(listOf(pending(A, arrivedAt = START - HOUR, parked = false)))
        val f = ready(Fixture(this, store))
        assertTrue("앱 시작 = PARKED 봄", store.pendingList.single().parkedSeen)
        drive(f, DrivingState.DRIVING)
        assertEquals(listOf("경복궁"), f.places.map { it.userPlaceNm })
        assertEquals(START - HOUR, f.places.single().recordedAt)
    }

    @Test
    fun `T13 대기 A(PARKED 아직) · 여행 종료 — 확정이 종료보다 먼저 · COMPLETED`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        var placesAtEnd = -1
        val endedAt = f.recorder.withEndTrip {
            placesAtEnd = f.places.size
            f.recording.endTrip()
        }
        assertNotNull(endedAt)
        assertEquals(1, placesAtEnd)
        assertEquals(TripStatus.COMPLETED, f.recording.progress.first().status)
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T13 AutoRecordEndTrip — 대기 확정 → 종료 · 종료 시각을 돌려줌`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        val endedAt = AutoRecordEndTrip(f.recorder, f.recording)()
        assertEquals(f.time.now, endedAt)
        assertEquals(1, f.places.size)
        assertEquals(TripStatus.COMPLETED, f.recording.progress.first().status)
    }

    @Test
    fun `T14 대기 A(PARKED 봄) · PAUSED — DRIVING · 수동 기록에도 유지 → 재개 · P → D 때 장소 A`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED)
        f.recording.togglePause()
        drive(f, DrivingState.DRIVING)
        f.recorder.withManualRecord(TRIP, null, null) { f.trips.addPlace(TRIP, null, draft = true) }
        assertEquals(1, f.store.pendingList.size)
        assertEquals(1, f.places.size)
        f.recording.togglePause()
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf(null, "경복궁"), f.places.sortedBy { it.placeOrd }.map { it.userPlaceNm })
    }

    @Test
    fun `T15 여행 삭제 · 모두 지우기 — 대기 0 · 자동 이름 원본 0`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        start(f, C)
        arrive(f, C.name)
        assertEquals(1, f.store.pendingList.size)
        assertEquals(1, f.store.nameList.size)
        f.recorder.deleteTrip(TRIP)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.store.nameList.isEmpty())

        f.store.pendingList = listOf(pending(A))
        f.store.nameList = listOf(AutoPlaceName(1L, "x"))
        f.recorder.deleteAll()
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.store.nameList.isEmpty())
    }

    /** 대기 · 진행 중 직접 안내 삭제 · 안 고친 자동 이름 null · 고친 이름 유지 · 주소 이름 null · 좌표 주소 null · 원본 0 · 띠 칸 그대로. */
    @Test
    fun `T16 위치 동의 철회 — 대기 · 자동 이름 삭제 · 고친 이름 유지`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        start(f, C)
        arrive(f, C.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        val (placeA, placeC) = f.places.sortedBy { it.placeOrd }
        f.trips.savePlace(placeC.tripPlaceNo, "할머니 댁", null, emptySet())
        val manualD = f.trips.addPlace(TRIP, CurrentLocation(37.0, 127.0, "강원 속초시 중앙로 147"), draft = true)
        f.trips.savePlace(manualD, "중앙로 147", null, emptySet())
        start(f, B)
        arrive(f, B.name)
        start(f, A) // 진행 중 직접 안내 A
        val unfinishedBefore = f.inputLocal.unfinishedInput.first()

        f.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = false)
        f.recorder.withdrawLocation()

        val byNo = f.places.associateBy { it.tripPlaceNo }
        assertNull(byNo.getValue(placeA.tripPlaceNo).userPlaceNm)
        assertEquals("할머니 댁", byNo.getValue(placeC.tripPlaceNo).userPlaceNm)
        assertNull(byNo.getValue(manualD).userPlaceNm)
        assertTrue(f.places.all { it.lat == null && it.lng == null && it.addr == null })
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.store.nameList.isEmpty())
        assertEquals(unfinishedBefore, f.inputLocal.unfinishedInput.first())

        // 이후 동의 없음 — 직접 안내가 지워져 도착해도 대기 0 · 조회도 0
        val queries = f.navi.destinationQueries
        arrive(f, A.name)
        start(f, D)
        arrive(f, D.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(f.store.pendingList.isEmpty())
        assertEquals(queries, f.navi.destinationQueries)
        assertEquals(3, f.places.size)
    }

    @Test
    fun `T17 PAUSED · 여행 끝남 중 직접 안내 도착 — 대기 0`() = runTest {
        val f = ready()
        start(f, A)
        f.recording.togglePause()
        arrive(f, A.name)
        assertTrue(f.store.pendingList.isEmpty())
        f.recording.togglePause()
        start(f, C)
        f.recording.endTrip()
        arrive(f, C.name)
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T18 동의 없음 — 목적지 조회 0 · 대기 0 · 장소 0`() = runTest {
        val f = ready(consent = false)
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(0, f.navi.destinationQueries)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T20 도착 이름이 다름(경유지 등) — 대기 0 · 이어 진짜 목적지 도착은 대기 1`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, "광화문")
        assertTrue(f.store.pendingList.isEmpty())
        arrive(f, A.name)
        assertEquals(1, f.store.pendingList.size)
    }

    @Test
    fun `T22 도착이 PARKED 뒤에 옴 → D — 대기 유지 → 다음 P → D 때 장소 A`() = runTest {
        val f = ready()
        start(f, A)
        drive(f, DrivingState.PARKED)
        arrive(f, A.name)
        drive(f, DrivingState.DRIVING)
        assertTrue(f.places.isEmpty())
        assertEquals(1, f.store.pendingList.size)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(1, f.places.size)
    }

    @Test
    fun `T23 목적지 조회 실패 · 안내 중 아님 · 좌표 (0, 0) — 장소 0`() = runTest {
        val f = ready()
        start(f, null)
        arrive(f, A.name)
        start(f, A.copy(latitude = 0.0, longitude = 0.0))
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T24 A 장소 있음 · 같은 A 다시 직접 시작 · 도착 · P · D — 장소 그대로 1행`() = runTest {
        val f = ready()
        f.trips.addPlace(TRIP, CurrentLocation(A.latitude, A.longitude, A.address), draft = true)
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(1, f.places.size)
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T25 대기 A · A'(80 m · 이름 다름) 둘 다 PARKED 봄 → D — 장소 1행(먼저 도착한 것)`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        val near = NaviDestination("국립고궁박물관", "서울 종로구 효자로 12", A.latitude + M80, A.longitude)
        start(f, near)
        f.time.now += 1_000
        arrive(f, near.name)
        assertEquals(2, f.store.pendingList.size)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf("경복궁"), f.places.map { it.userPlaceNm })
    }

    @Test
    fun `T27 앱 요청 X 요청 중(내부 칸 X) · 같은 X 시작 알림 → X 도착 → P → D — 이 앱 요청 · 장소 0`() = runTest {
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        start(f, X)
        arrive(f, X.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T28 앱 요청 X 시작됨 뒤 프로세스 새로 시작(입구 기억 없음) · X 시작 알림(재탐색) → 도착 · P · D — 직접 시작 · 장소 X 1행`() = runTest {
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        assertEquals(1, f.pins.recorded.size)
        f.restartProcess()
        start(f, X)
        arrive(f, X.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf(X.name), f.places.map { it.userPlaceNm })
        assertEquals("핀은 그대로", 1, f.pins.recorded.size)
    }

    @Test
    fun `T29 앱 요청 X 시작됨 10초 뒤 직접 B(1 km) 시작 → B 도착 → P → D — 직접 시작 · 장소 B`() = runTest {
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        f.time.now += 10_000
        val b = NaviDestination("B", "서울 중구 세종대로 110", X.latitude + KM, X.longitude)
        start(f, b)
        arrive(f, b.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf("B"), f.places.map { it.userPlaceNm })
    }

    @Test
    fun `T30 이름 · 주소 빈 문자열 — 장소 1행 · 이름 · 주소 null · 좌표 있음 · 원본 없음`() = runTest {
        val f = ready()
        val blank = NaviDestination(" ", "", A.latitude, A.longitude)
        start(f, blank)
        arrive(f, "")
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        val place = f.places.single()
        assertNull(place.userPlaceNm)
        assertNull(place.addr)
        assertNotNull(place.lat)
        assertTrue(f.store.nameList.isEmpty())
    }

    @Test
    fun `T31 이어서 쓰기 칸에 작성 중 내용 있음 — 확정해도 칸 그대로`() = runTest {
        val f = ready()
        f.inputs.saveNow("records", mapOf("memo" to "쓰던 글"))
        val before = f.inputLocal.unfinishedInput.first()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(1, f.places.size)
        assertEquals(before, f.inputLocal.unfinishedInput.first())
    }

    @Test
    fun `T33 얼림 중 쌓인 도착 · P · D 가 복귀 때 차례로 옴 — 장소 1행 · 기록 시각 = 도착 알림 받은 때`() = runTest {
        val f = ready()
        start(f, A)
        f.time.now += 5 * 60_000
        val received = f.time.now
        f.navi.naviEvents.emit(NaviEvent.DestinationArrived(A.name))
        f.driving.emit(DrivingState.PARKED)
        runCurrent()
        f.driving.emit(DrivingState.DRIVING)
        runCurrent()
        assertEquals(received, f.places.single().recordedAt)
    }

    @Test
    fun `T35 전 과정 — 내비 조회는 시작 알림 때만(도착 · 주행 판정 변화 · 확정에 조회 0)`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING, DrivingState.PARKED)
        advanceTimeBy(HOUR)
        runCurrent()
        assertEquals(1, f.navi.destinationQueries)
    }

    @Test
    fun `T42 대기 A(PARKED 봄 · 도착 25시간 전) · 프로세스 새로 시작 → DRIVING — 대기 폐기 · 장소 0`() = runTest {
        val store = FakeAutoRecordLocalDataSource(listOf(pending(A, arrivedAt = START - 25 * HOUR, parked = true)))
        val f = ready(Fixture(this, store))
        drive(f, DrivingState.DRIVING)
        assertTrue(store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T43 대기 A(PARKED 봄 · 도착 23시간 59분 전) · 1분 뒤 DRIVING — 대기 폐기 · 장소 0(경계)`() = runTest {
        val store = FakeAutoRecordLocalDataSource(listOf(pending(A, arrivedAt = START - 24 * HOUR + 60_000, parked = true)))
        val f = ready(Fixture(this, store))
        assertEquals(1, store.pendingList.size)
        f.time.now += 60_000
        drive(f, DrivingState.DRIVING)
        assertTrue(store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `T44 대기 A(PARKED 아직) · 같은 A 재탐색 시작(30 m) — 유지 → P → D → 장소 A`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        start(f, A.copy(latitude = A.latitude + M30))
        assertEquals(1, f.store.pendingList.size)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf("경복궁"), f.places.map { it.userPlaceNm })
    }

    @Test
    fun `T45 대기 A(PARKED 봄) · 다른 C 시작 — 유지 → D → 장소 A`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED)
        start(f, C)
        assertEquals(1, f.store.pendingList.size)
        drive(f, DrivingState.DRIVING)
        assertEquals(listOf("경복궁"), f.places.map { it.userPlaceNm })
    }

    @Test
    fun `T46 시작 처리 중(1·5초 대기 · 조회 중) 도착 — 시작 처리가 끝난 뒤 도착 처리 · 대기 1`() = runTest {
        val f = ready()
        val gate = CompletableDeferred<Unit>()
        f.navi.destinationGate = gate
        f.navi.destination = A
        f.navi.naviEvents.emit(NaviEvent.RouteStarted)
        runCurrent()
        f.navi.naviEvents.emit(NaviEvent.DestinationArrived(A.name))
        advanceTimeBy(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 1)
        runCurrent()
        assertTrue("조회가 끝나기 전엔 도착을 처리하지 않음", f.store.pendingList.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, f.store.pendingList.size)
    }

    /** 대기 A(PARKED 봄) · 500 m 떨어진 카페 C 에서 수동 기록 — 장소 A(n+1 · 기록 시각 = 도착 시각) → C(n+2) · 띠 칸은 자동이 쓰지 않음. */
    @Test
    fun `T48 대기 A · 500 m 밖 수동 기록 — A 먼저 확정 → 수동 C`() = runTest {
        val f = ready()
        start(f, A)
        val arrivedAt = f.time.now
        arrive(f, A.name)
        drive(f, DrivingState.PARKED)
        f.time.now += 600_000
        val cafe = CurrentLocation(A.latitude + M500, A.longitude, "서울 종로구 자하문로 1")
        f.recorder.withManualRecord(TRIP, cafe.lat, cafe.lng) { f.trips.addPlace(TRIP, cafe, draft = true) }
        val ordered = f.places.sortedBy { it.placeOrd }
        assertEquals(listOf("경복궁", "서울 종로구 자하문로 1"), ordered.map { it.userPlaceNm })
        assertEquals(arrivedAt, ordered.first().recordedAt)
        assertNull(f.inputLocal.unfinishedInput.first())
        assertTrue(f.store.pendingList.isEmpty())
    }

    @Test
    fun `T49 대기 A(PARKED 아직) · 수동 위치를 못 읽은 기록 — 장소 A 먼저 → 수동 행(위치 없음)`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        f.recorder.withManualRecord(TRIP, null, null) { f.trips.addPlace(TRIP, null, draft = true) }
        assertEquals(listOf("경복궁", null), f.places.sortedBy { it.placeOrd }.map { it.userPlaceNm })
    }

    @Test
    fun `T50 attach 두 번 — 같은 시작 알림을 한 번만 처리`() = runTest {
        val f = ready()
        f.recorder.attach()
        runCurrent()
        start(f, A)
        assertEquals(1, f.navi.destinationQueries)
    }

    @Test
    fun `T53 앱 요청 X 시작됨 3시간 뒤 직접 X 시작 → 도착 · P · D — 직접 시작 · 장소 X 1행 · 핀 그대로`() = runTest {
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        f.time.now += 3 * HOUR
        start(f, X)
        arrive(f, X.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf(X.name), f.places.map { it.userPlaceNm })
        assertEquals(1, f.pins.recorded.size)
    }

    @Test
    fun `T47 같은 시작 알림 — (a) 입구가 먼저 받아 시작됨 뒤 (b) 이 기능이 먼저 받음 — 둘 다 이 앱 요청 · 장소 0 · 핀 1건`() = runTest {
        // (a) 입구가 먼저 — 시작됨(X) · 내부 칸 비고 시작됨 한 칸 = X · 지금
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        start(f, X)
        arrive(f, X.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(f.places.isEmpty())
        assertEquals(1, f.pins.recorded.size)

        // (b) 이 기능이 먼저 — 내부 칸 X 남음(요청 중)
        val g = ready(Fixture(this))
        g.guide.guide(X_PLACE)
        runCurrent()
        start(g, X)
        g.navi.answerRoute(RouteRequestResult.Started)
        runCurrent()
        arrive(g, X.name)
        drive(g, DrivingState.PARKED, DrivingState.DRIVING)
        assertTrue(g.places.isEmpty())
        assertEquals(1, g.pins.recorded.size)
    }

    @Test
    fun `T57 TIMEOUT 뒤 기억 기한 지남 · 상태 = 실패(TIMEOUT · X) — 직접 X 시작 → 장소 X 1행 · 핀 없음`() = runTest {
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        advanceTimeBy(AppConfig.ROUTE_START_TIMEOUT_MILLIS + AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 2)
        runCurrent()
        advanceTimeBy(AppConfig.TIMED_OUT_REQUEST_MEMORY_MILLIS + 1)
        runCurrent()
        start(f, X)
        arrive(f, X.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf(X.name), f.places.map { it.userPlaceNm })
        assertTrue(f.pins.recorded.isEmpty())
    }

    @Test
    fun `T58 앱 요청 X 일반 실패(ROUTE_NOT_FOUND) — 직접 X 시작 → 장소 X 1행`() = runTest {
        val f = ready()
        f.guide.guide(X_PLACE)
        runCurrent()
        f.navi.answerRoute(RouteRequestResult.Failed(com.example.travelcrumbs.domain.model.RouteFailReason.ROUTE_NOT_FOUND))
        runCurrent()
        start(f, X)
        arrive(f, X.name)
        drive(f, DrivingState.PARKED, DrivingState.DRIVING)
        assertEquals(listOf(X.name), f.places.map { it.userPlaceNm })
    }

    @Test
    fun `대기 trip 이 지금 여행이 아님(다른 여행 시작) — 재출발 때 폐기`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED)
        f.recordingLocal.update { it?.copy(tripNo = TRIP + 1) }
        drive(f, DrivingState.DRIVING)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    @Test
    fun `리뷰 심사 1 조회(1·5초 + SDK) 중에 철회 · 여행 종료가 바로 끝남 · 늦은 조회 결과는 버림`() = runTest {
        val f = ready()
        val gate = CompletableDeferred<Unit>()
        f.navi.destinationGate = gate
        f.navi.destination = A
        f.store.pendingList = listOf(pending(C, arrivedAt = START))
        f.navi.naviEvents.emit(NaviEvent.RouteStarted)
        advanceTimeBy(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS + 1)
        runCurrent()
        assertEquals("조회 중", 1, f.navi.destinationQueries)

        val withdrawn = launch { f.recorder.withdrawLocation() }
        runCurrent()
        assertTrue("철회가 조회를 기다리지 않음", withdrawn.isCompleted)
        assertTrue(f.store.pendingList.isEmpty())

        f.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        val ended = launch { AutoRecordEndTrip(f.recorder, f.recording)() }
        runCurrent()
        assertTrue("종료가 조회를 기다리지 않음", ended.isCompleted)

        gate.complete(Unit)
        runCurrent()
        f.recordingLocal.update { CurrentTrip.start(f.time.now).copy(tripNo = TRIP) }
        arrive(f, A.name)
        assertTrue("철회 전에 시작한 조회 결과는 직접 안내가 되지 않음", f.store.pendingList.isEmpty())
    }

    @Test
    fun `리뷰 심사 1 차례 대기 중 주행 판정 3번 — 다른 구독자는 막힘 없이 셋 다 받음 · 기록기도 뒤에 차례로 처리`() = runTest {
        val shared = MutableSharedFlow<DrivingState>(replay = 1)
        val sharedDriving = object : DrivingSignalRepository {
            override val drivingState = shared
        }
        val f = Fixture(this)
        f.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        val recorder = AutoPlaceRecorder(f.navi, sharedDriving, f.recording, f.users, f.trips, f.inputs, f.guide, f.store, f.time, backgroundScope)
        f.store.pendingList = listOf(pending(A, arrivedAt = START, parked = false))
        recorder.attach()
        runCurrent()
        val other = mutableListOf<DrivingState>()
        backgroundScope.launch { shared.collect { other += it } }
        runCurrent()

        val hold = CompletableDeferred<Unit>()
        launch { recorder.withEndTrip { hold.await() } } // 차례를 오래 잡음
        runCurrent()
        val emitter = launch {
            shared.emit(DrivingState.PARKED)
            shared.emit(DrivingState.DRIVING)
            shared.emit(DrivingState.PARKED)
        }
        runCurrent()
        assertTrue("내보내는 쪽이 기록기 처리를 기다리지 않음", emitter.isCompleted)
        assertEquals(listOf(DrivingState.PARKED, DrivingState.DRIVING, DrivingState.PARKED), other)
        hold.complete(Unit)
        runCurrent()
    }

    @Test
    fun `리뷰 심사 2 철회 때 장소 정리가 실패해도 대기 · 원본은 지워짐`() = runTest {
        val f = ready()
        f.store.pendingList = listOf(pending(A))
        f.store.nameList = listOf(AutoPlaceName(1L, "경복궁"))
        f.trips.clearLocationsError = IllegalStateException("db")
        val result = runCatching { f.recorder.withdrawLocation() }
        assertTrue(result.isFailure)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.store.nameList.isEmpty())
    }

    @Test
    fun `리뷰 심사 2 동의 없음 상태의 재출발 — 대기 폐기 · 장소 0`() = runTest {
        val f = ready()
        start(f, A)
        arrive(f, A.name)
        drive(f, DrivingState.PARKED)
        f.users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = false)
        drive(f, DrivingState.DRIVING)
        assertTrue(f.store.pendingList.isEmpty())
        assertTrue(f.places.isEmpty())
    }

    private companion object {
        const val START = 1_700_000_000_000L
        const val HOUR = 60 * 60_000L
        const val TRIP = 7L

        /** 위도 차 → 대략 거리. */
        const val M30 = 0.00027
        const val M50 = 0.00045
        const val M80 = 0.00072
        const val M500 = 0.0045
        const val KM = 0.009

        val A = NaviDestination("경복궁", "서울 종로구 사직로 161", 37.5796, 126.9770)
        val B = NaviDestination("광장시장", "서울 종로구 창경궁로 88", 37.5700, 126.9996)
        val C = NaviDestination("남산서울타워", "서울 용산구 남산공원길 105", 37.5512, 126.9882)
        val D = NaviDestination("서울숲", "서울 성동구 뚝섬로 273", 37.5444, 127.0374)
        val X = NaviDestination("X 이름", "X 주소", 37.5, 127.0)
        val X_PLACE = Place(id = "x", name = "X 이름", address = "X 주소", latitude = 37.5, longitude = 127.0)

        fun trip(tripNo: Long) = TripRecord(tripNo, "시험 여행", TripStatus.RECORDING, Visibility.PRIVATE, START, null, null, null, emptySet(), emptyList())

        fun pending(d: NaviDestination, arrivedAt: Long = START, parked: Boolean = false) =
            PendingArrival(TRIP, d.name, d.address, d.latitude, d.longitude, arrivedAt, parked)
    }
}
