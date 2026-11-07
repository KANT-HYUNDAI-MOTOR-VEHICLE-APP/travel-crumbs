package com.example.travelcrumbs.data.recording

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.RecordingProgress
import com.example.travelcrumbs.domain.model.TripStatus
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 실제 DataStore(임시 파일)로 #6 판 저장 값 옮기기(N10)와 새 키 저장을 확인. */
class RecordingLocalDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val minute = AppConfig.ELAPSED_UNIT_MILLIS
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val time = FakeTimeSource(now = 1_000 * minute)
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(folder.root, "${AppPreferencesKeys.FILE_NAME}.preferences_pb") },
    )
    private val local = DataStoreRecordingLocalDataSource(dataStore, time)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun seedLegacy(status: String, accumulated: Long, segmentStart: Long) = runBlocking {
        dataStore.edit {
            it[AppPreferencesKeys.Legacy.RECORDING_STATUS] = status
            it[AppPreferencesKeys.Legacy.RECORDING_ACCUMULATED_MILLIS] = accumulated
            it[AppPreferencesKeys.Legacy.RECORDING_SEGMENT_START_MILLIS] = segmentStart
        }
    }

    @Test
    fun `V23 #6 판 PAUSED 누적 30분 — 실행하면 RECORDING · 경과 30분부터 이어짐 · 옛 키 지워짐`() = runBlocking {
        seedLegacy(status = "PAUSED", accumulated = 30 * minute, segmentStart = 5 * minute)

        val progress = DefaultRecordingRepository(local, time).progress.first()

        assertEquals(RecordingProgress(TripStatus.RECORDING, 30), progress)
        val prefs = dataStore.data.first()
        assertNull(prefs[AppPreferencesKeys.Legacy.RECORDING_STATUS])
        assertNull(prefs[AppPreferencesKeys.Legacy.RECORDING_ACCUMULATED_MILLIS])
        assertEquals(TripStatus.RECORDING.name, prefs[AppPreferencesKeys.TRIP_STATUS])

        time.now += 5 * minute
        assertEquals(35L, DefaultRecordingRepository(local, time).progress.first().elapsedMinutes)
    }

    @Test
    fun `V23 #6 판 RECORDING — 시작 시각 = 구간 시작 − 누적 · 앱이 꺼져 있던 시간 포함`() = runBlocking {
        seedLegacy(status = "RECORDING", accumulated = 10 * minute, segmentStart = time.now - 20 * minute)

        val progress = DefaultRecordingRepository(local, time).progress.first()

        assertEquals(RecordingProgress(TripStatus.RECORDING, 30), progress)
    }

    @Test
    fun `V22 지금 여행 요약을 새 키에 남기고 다시 읽음 — 종료 시각 · 여행명 없음 포함`() = runBlocking {
        val completed = CurrentTrip.start(2 * minute).ended(9 * minute)

        local.update { completed }

        assertEquals(completed, local.trip.first())
        assertNull(dataStore.data.first()[AppPreferencesKeys.TRIP_NAME])
    }

    @Test
    fun `N10 알 수 없는 옛 값이면 첫 실행처럼 새 여행`() {
        assertNull(legacyRecordingToTrip("SOMETHING", 0, 0, 0))
    }
}
