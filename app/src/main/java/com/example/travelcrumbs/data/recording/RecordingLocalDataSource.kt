package com.example.travelcrumbs.data.recording

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.model.RecordingSession
import com.example.travelcrumbs.domain.model.RecordingStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 기록 상태를 기기 안에 남김 — 앱 종료 · 재부팅 뒤 복원(스펙 app-shell D3). */
interface RecordingLocalDataSource {
    val session: Flow<RecordingSession?>

    /** 읽고 바꾸기를 한 번에(원자적) — 두 화면이 거의 동시에 눌러도 어긋나지 않게. */
    suspend fun update(transform: (RecordingSession?) -> RecordingSession)
}

@Singleton
class DataStoreRecordingLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : RecordingLocalDataSource {

    override val session: Flow<RecordingSession?> = dataStore.data.map { it.toSession() }

    override suspend fun update(transform: (RecordingSession?) -> RecordingSession) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSession())
            prefs[AppPreferencesKeys.RECORDING_STATUS] = next.status.name
            prefs[AppPreferencesKeys.RECORDING_ACCUMULATED_MILLIS] = next.accumulatedMillis
            prefs[AppPreferencesKeys.RECORDING_SEGMENT_START_MILLIS] = next.segmentStartMillis
        }
    }

    private fun Preferences.toSession(): RecordingSession? {
        val status = this[AppPreferencesKeys.RECORDING_STATUS]
            ?.let { name -> RecordingStatus.entries.firstOrNull { it.name == name } }
            ?: return null
        return RecordingSession(
            status = status,
            accumulatedMillis = this[AppPreferencesKeys.RECORDING_ACCUMULATED_MILLIS] ?: 0L,
            segmentStartMillis = this[AppPreferencesKeys.RECORDING_SEGMENT_START_MILLIS] ?: 0L,
        )
    }
}
