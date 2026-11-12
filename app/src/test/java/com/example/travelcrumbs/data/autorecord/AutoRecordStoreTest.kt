package com.example.travelcrumbs.data.autorecord

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.example.travelcrumbs.data.local.AppDatabase
import com.example.travelcrumbs.data.recording.DataStoreRecordingLocalDataSource
import com.example.travelcrumbs.data.recording.DefaultRecordingRepository
import com.example.travelcrumbs.data.recording.RoomRecordingLocalDataSource
import com.example.travelcrumbs.data.trip.LocalTripRepository
import com.example.travelcrumbs.data.user.LocalCurrentUserRepository
import com.example.travelcrumbs.domain.model.AutoPlace
import com.example.travelcrumbs.domain.model.AutoPlaceName
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.NewTrip
import com.example.travelcrumbs.domain.model.PendingArrival
import com.example.travelcrumbs.testing.FakeTimeSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 자동 기록의 기기 안 저장(#66 4.2) · 확정 행(4.1) · 철회 때 이름 비교(3.9) — 실제 Room · DataStore 파일. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AutoRecordStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Application = RuntimeEnvironment.getApplication()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val time = FakeTimeSource(now = 1_000_000L)
    private var database: AppDatabase? = null
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "auto.preferences_pb") })
    }

    private fun db(): AppDatabase = database ?: Room.databaseBuilder(context, AppDatabase::class.java, FILE)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .allowMainThreadQueries()
        .build()
        .also { database = it }

    private val users by lazy { LocalCurrentUserRepository(db().travelDao(), time) }
    private val trips by lazy { LocalTripRepository(db().travelDao(), users, dataStore, time) }
    private val recording by lazy {
        DefaultRecordingRepository(
            RoomRecordingLocalDataSource(db(), db().travelDao(), users, DataStoreRecordingLocalDataSource(dataStore, time), time),
            time,
        )
    }
    private val store by lazy { DataStoreAutoRecordLocalDataSource(dataStore, json) }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(FILE)
        scope.cancel()
    }

    @Test
    fun `4-2 대기 · 원본 저장 — 다시 읽어도 같음(프로세스 종료를 넘김) · 모두 지우면 빈 목록`() = runBlocking {
        val pending = PendingArrival(3L, "경복궁", null, 37.5796, 126.977, 900L, parkedSeen = true)
        store.savePendings(listOf(pending))
        store.saveNames(listOf(AutoPlaceName(11L, "경복궁")))
        val again = DataStoreAutoRecordLocalDataSource(dataStore, json)
        assertEquals(listOf(pending), again.pendings())
        assertEquals(listOf(AutoPlaceName(11L, "경복궁")), again.names())
        again.clearAll()
        assertTrue(store.pendings().isEmpty())
        assertTrue(store.names().isEmpty())
    }

    @Test
    fun `T3 4-1 확정 행 — draft Y · 순서 = 다음 번호 · 기록 시각 = 도착 시각 · 메모 없음`() = runBlocking {
        recording.startTrip(NewTrip("서울", emptySet(), null))
        val tripNo = recording.progress.first().tripNo!!
        trips.addPlace(tripNo, CurrentLocation(37.57, 126.97, "서울 종로구 세종대로 1"), draft = true)
        time.now = 5_000_000L
        val no = trips.addAutoPlace(tripNo, AutoPlace("경복궁", "서울 종로구 사직로 161", 37.5796, 126.977, recordedAt = 4_000_000L))
        val place = trips.trip(tripNo).first()!!.places.single { it.tripPlaceNo == no }
        assertEquals(2, place.placeOrd)
        assertTrue(place.draft)
        assertEquals(4_000_000L, place.recordedAt)
        assertNull(place.memo)
        assertEquals("경복궁", place.userPlaceNm)
        assertEquals(2, recording.progress.first().placeCount)
    }

    @Test
    fun `T16 철회 — 안 고친 자동 이름 null · 고친 자동 이름 유지 · 주소 처음 이름 null · 고친 수동 이름 유지 · 좌표 주소 null`() = runBlocking {
        recording.startTrip(NewTrip("서울", emptySet(), null))
        val tripNo = recording.progress.first().tripNo!!
        val autoA = trips.addAutoPlace(tripNo, AutoPlace("경복궁", "서울 종로구 사직로 161", 37.5796, 126.977, 1L))
        val autoC = trips.addAutoPlace(tripNo, AutoPlace("남산서울타워", "서울 용산구 남산공원길 105", 37.55, 126.98, 2L))
        trips.savePlace(autoC, "할머니 댁", null, emptySet())
        val manualD = trips.addPlace(tripNo, CurrentLocation(38.2, 128.59, "강원 속초시 중앙로 147"), draft = true)
        val manualE = trips.addPlace(tripNo, CurrentLocation(38.3, 128.6, "강원 속초시 영랑호반길 1"), draft = true)
        trips.savePlace(manualE, "우리 단골집", null, emptySet())

        trips.clearLocations(mapOf(autoA to "경복궁", autoC to "남산서울타워"))

        val byNo = trips.trip(tripNo).first()!!.places.associateBy { it.tripPlaceNo }
        assertNull(byNo.getValue(autoA).userPlaceNm)
        assertEquals("할머니 댁", byNo.getValue(autoC).userPlaceNm)
        assertNull(byNo.getValue(manualD).userPlaceNm)
        assertEquals("우리 단골집", byNo.getValue(manualE).userPlaceNm)
        assertTrue(byNo.values.all { it.lat == null && it.lng == null && it.addr == null })
    }

    private companion object {
        const val FILE = "auto-record-store-test.db"
    }
}
