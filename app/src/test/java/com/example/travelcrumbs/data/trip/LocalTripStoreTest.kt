package com.example.travelcrumbs.data.trip

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import com.example.travelcrumbs.data.local.AppDatabase
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.data.recording.DataStoreRecordingLocalDataSource
import com.example.travelcrumbs.data.recording.DefaultRecordingRepository
import com.example.travelcrumbs.data.recording.RoomRecordingLocalDataSource
import com.example.travelcrumbs.data.user.LocalCurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.NewTrip
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.FakeTimeSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 기기 안 여행 표(#51 U1 · Room 판 3) — 실제 DB 파일 · DataStore 임시 파일로 저장소 · 마이그레이션을 확인. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocalTripStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Application = RuntimeEnvironment.getApplication()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val time = FakeTimeSource(now = 1_000_000L)
    private var database: AppDatabase? = null

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "test.preferences_pb") })
    }

    private fun db(): AppDatabase = database ?: Room.databaseBuilder(context, AppDatabase::class.java, FILE)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .allowMainThreadQueries()
        .build()
        .also { database = it }

    private val users by lazy { LocalCurrentUserRepository(db().travelDao(), time) }
    private val trips by lazy { LocalTripRepository(db().travelDao(), users, dataStore, time) }
    private val legacy by lazy { DataStoreRecordingLocalDataSource(dataStore, time) }
    private val recording by lazy {
        DefaultRecordingRepository(RoomRecordingLocalDataSource(db(), db().travelDao(), users, legacy, time), time)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(FILE)
        scope.cancel()
    }

    @Test
    fun `U1-2 판 2 기기 업데이트 — 핀 표 그대로 · 여행 표 7개가 빈 채로 생김`() = runBlocking {
        context.openOrCreateDatabase(FILE, Context.MODE_PRIVATE, null).use { raw ->
            raw.execSQL(V2_CREATE)
            raw.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            raw.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$V2_HASH')")
            raw.execSQL("INSERT INTO navi_req_log VALUES (1, 'a', 1000, 32400, 'SENT')")
            raw.version = 2
        }

        assertEquals(listOf("a"), db().naviReqLogDao().observeAll().first().map { it.placeId })
        assertTrue(db().travelDao().trips().first().isEmpty())
        assertTrue(db().travelDao().places().first().isEmpty())
    }

    @Test
    fun `U1-3 기본 사용자 — 처음 읽으면 1번 · 「여행자」 · 닉네임 공백은 지우고 빈 값은 거절`() = runBlocking {
        assertEquals(AppConfig.LOCAL_USER_NO, users.user.first().userNo)
        assertEquals(AppConfig.DEFAULT_NICKNAME_NM, users.user.first().nicknameNm)

        assertTrue(users.updateNickname("  민지네 "))
        assertEquals("민지네", users.user.first().nicknameNm)
        assertFalse(users.updateNickname("   "))
        assertEquals("민지네", users.user.first().nicknameNm)
    }

    @Test
    fun `U1-4 동의 이력 — 유형별 최신 행이 지금 상태 · 철회도 행으로 쌓임`() = runBlocking {
        assertTrue(users.consents.first().isEmpty())
        users.recordConsent(ConsentType.LOCATION_COLLECT, agreed = true)
        users.recordConsent(ConsentType.RECO_PUBLIC, agreed = true)
        time.now += 10
        users.recordConsent(ConsentType.RECO_PUBLIC, agreed = false)

        val latest = users.consents.first()
        assertTrue(latest.getValue(ConsentType.LOCATION_COLLECT).agreed)
        assertFalse(latest.getValue(ConsentType.RECO_PUBLIC).agreed)
        assertEquals(3, db().travelDao().consents(AppConfig.LOCAL_USER_NO).first().size)
    }

    @Test
    fun `U1-5 여행 시작 · 종료 — trip_mst 기록 중 · 비공개 · 동행자 행 · 종료면 완료 · 기록 시간(초)`() = runBlocking {
        assertFalse(recording.progress.first().hasTrip)

        assertTrue(recording.startTrip(NewTrip("속초 가족여행", setOf(CompanionType.SPOUSE, CompanionType.CHILD), "속초 중앙시장")))
        val started = recording.progress.first()
        assertEquals(TripStatus.RECORDING, started.status)
        assertEquals("속초 중앙시장", started.destPlaceNm)
        val tripNo = started.tripNo!!

        val row = trips.trip(tripNo).first()!!
        assertEquals(Visibility.PRIVATE, row.visibility)
        assertEquals(setOf(CompanionType.SPOUSE, CompanionType.CHILD), row.companions)

        time.now += 30 * 60_000L
        assertEquals(time.now, recording.endTrip())
        val ended = trips.trip(tripNo).first()!!
        assertEquals(TripStatus.COMPLETED, ended.status)
        assertEquals(30 * 60_000L, ended.durationMillis)
    }

    @Test
    fun `U1-6 옛 DataStore 진행 중 여행 — 처음 쓸 때 trip_mst 로 옮기고 DataStore 값은 지움`() = runBlocking {
        dataStore.edit {
            it[AppPreferencesKeys.TRIP_STATUS] = TripStatus.PAUSED.name
            it[AppPreferencesKeys.TRIP_NAME] = "옛 여행"
            it[AppPreferencesKeys.TRIP_START_AT_MILLIS] = 0L
            it[AppPreferencesKeys.TRIP_PLACE_COUNT] = 0
            it[AppPreferencesKeys.TRIP_ACCUMULATED_MILLIS] = 15 * 60_000L
            it[AppPreferencesKeys.TRIP_SEGMENT_START_MILLIS] = 0L
        }

        val progress = recording.progress.first()

        assertEquals(TripStatus.PAUSED, progress.status)
        assertEquals("옛 여행", progress.tripName)
        assertEquals(15L, progress.elapsedMinutes)
        assertNull(dataStore.data.first()[AppPreferencesKeys.TRIP_STATUS])
    }

    @Test
    fun `U1-7 장소 · 태그 · 공개 — 임시저장 장소 순서 · 태그 저장 · 공개 · 철회면 모두 비공개 · 검토 대기`() = runBlocking {
        recording.startTrip(NewTrip("속초", emptySet(), null))
        val tripNo = recording.progress.first().tripNo!!

        val first = trips.addPlace(tripNo, CurrentLocation(38.2, 128.59, "강원 속초시 중앙로 147"), draft = true)
        val second = trips.addPlace(tripNo, null, draft = true)
        var trip = trips.trip(tripNo).first()!!
        assertEquals(listOf(1, 2), trip.places.map { it.placeOrd })
        assertEquals("중앙로 147", trip.places[0].userPlaceNm)
        assertEquals(2, trip.draftCnt)
        assertEquals(2, recording.progress.first().placeCount)

        trips.savePlace(first, "속초 중앙시장 주차장", "지하 2층", setOf(2L, 3L))
        trip = trips.trip(tripNo).first()!!
        assertEquals(listOf(2L, 3L), trip.places[0].tagNos)
        assertFalse(trip.places[0].draft)
        assertEquals(1, trip.draftCnt)
        assertTrue(trips.trip(tripNo).first()!!.places.any { it.tripPlaceNo == second && it.draft })

        assertNull("진행 중엔 검토 없음", trips.pendingReviewTripNo.first())
        recording.endTrip()
        assertEquals(tripNo, trips.pendingReviewTripNo.first())
        trips.markReviewed(tripNo)
        assertNull(trips.pendingReviewTripNo.first())

        trips.setVisibility(tripNo, Visibility.ANONYMOUS_PUBLIC)
        assertEquals(Visibility.ANONYMOUS_PUBLIC, trips.trip(tripNo).first()!!.visibility)
        trips.makeAllPrivate()
        assertEquals(Visibility.PRIVATE, trips.trip(tripNo).first()!!.visibility)
    }

    @Test
    fun `U1-8 태그 마스터 — 처음 읽으면 시드 5개 · sort_ord 순 · 두 번 읽어도 그대로`() = runBlocking {
        val tags = LocalTagRepository(db().travelDao(), time)
        val first = tags.tags.first()
        assertEquals(5, first.size)
        assertEquals("아이와 가기 좋아요", first.first().tagNm)
        assertEquals(first, tags.tags.first())
    }

    @Test
    fun `U1-9 주소 → 장소 이름 처음 값 — 시도 · 시군구를 뺀 나머지 · 짧으면 그대로`() {
        assertEquals("중앙로 147", placeNameFromAddr("강원 속초시 중앙로 147"))
        assertEquals("강원 속초시", placeNameFromAddr("강원 속초시"))
        assertNull(placeNameFromAddr("  "))
    }

    private companion object {
        const val FILE = "trip-store-test.db"
        const val V2_HASH = "4b1ffb2595401a5e9e9fe501ca5621b8"
        const val V2_CREATE =
            "CREATE TABLE IF NOT EXISTS `navi_req_log` (`navi_req_log_no` INTEGER NOT NULL, `placeId` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `utcOffsetSeconds` INTEGER NOT NULL, `uploadState` TEXT NOT NULL, " +
                "PRIMARY KEY(`navi_req_log_no`))"
    }
}
