package com.example.travelcrumbs.data.recording

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.data.local.AppPreferencesKeys.Legacy
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.CurrentTrip
import com.example.travelcrumbs.domain.model.TripStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 지금 여행 요약을 기기 안에 남김 — 앱 종료 · 재부팅 뒤 복원(#6 D3 · 스펙 app-shell-v5 4-1). */
interface RecordingLocalDataSource {
    /** 저장된 지금 여행 요약 — 없으면 null(#6 판 값만 있어도 null · [update] 가 옮김). */
    val trip: Flow<CurrentTrip?>

    /**
     * 읽고 바꾸기를 한 번에(원자적) — 두 화면이 거의 동시에 눌러도 어긋나지 않게.
     * 새 키가 비어 있고 #6 판 값이 있으면 그 값을 옮겨 [transform] 에 넘기고 옛 키를 지움(N10).
     */
    suspend fun update(transform: (CurrentTrip?) -> CurrentTrip)
}

@Singleton
class DataStoreRecordingLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeSource: TimeSource,
) : RecordingLocalDataSource {

    override val trip: Flow<CurrentTrip?> = dataStore.data.map { it.toTrip() }

    override suspend fun update(transform: (CurrentTrip?) -> CurrentTrip) {
        dataStore.edit { prefs ->
            val current = prefs.toTrip() ?: prefs.legacyTrip(timeSource.nowMillis())
            prefs.write(transform(current))
            prefs.remove(Legacy.RECORDING_STATUS)
            prefs.remove(Legacy.RECORDING_ACCUMULATED_MILLIS)
            prefs.remove(Legacy.RECORDING_SEGMENT_START_MILLIS)
        }
    }

    private fun MutablePreferences.write(trip: CurrentTrip) {
        this[AppPreferencesKeys.TRIP_STATUS] = trip.status.name
        this[AppPreferencesKeys.TRIP_START_AT_MILLIS] = trip.startAtMillis
        this[AppPreferencesKeys.TRIP_PLACE_COUNT] = trip.placeCount
        val end = trip.endAtMillis
        if (end == null) remove(AppPreferencesKeys.TRIP_END_AT_MILLIS) else this[AppPreferencesKeys.TRIP_END_AT_MILLIS] = end
        val name = trip.tripName
        if (name == null) remove(AppPreferencesKeys.TRIP_NAME) else this[AppPreferencesKeys.TRIP_NAME] = name
    }

    private fun Preferences.toTrip(): CurrentTrip? {
        val status = this[AppPreferencesKeys.TRIP_STATUS]
            ?.let { name -> TripStatus.entries.firstOrNull { it.name == name } }
            ?: return null
        return CurrentTrip(
            tripName = this[AppPreferencesKeys.TRIP_NAME],
            status = status,
            startAtMillis = this[AppPreferencesKeys.TRIP_START_AT_MILLIS] ?: 0L,
            endAtMillis = this[AppPreferencesKeys.TRIP_END_AT_MILLIS],
            placeCount = this[AppPreferencesKeys.TRIP_PLACE_COUNT] ?: 0,
        )
    }

    private fun Preferences.legacyTrip(nowMillis: Long): CurrentTrip? {
        val status = this[Legacy.RECORDING_STATUS] ?: return null
        return legacyRecordingToTrip(
            status = status,
            accumulatedMillis = this[Legacy.RECORDING_ACCUMULATED_MILLIS] ?: 0L,
            segmentStartMillis = this[Legacy.RECORDING_SEGMENT_START_MILLIS] ?: nowMillis,
            nowMillis = nowMillis,
        )
    }
}

/**
 * #6 판 기록 상태(RECORDING · PAUSED + 누적 경과 + 구간 시작) → 지금 여행 요약(N10 추천 (a)).
 * 기록 중이었으면 「구간 시작 − 누적」이 곧 시작 시각 · 일시정지였으면 지금부터 누적만큼 거슬러 올라간 시각
 * → 경과가 저장 당시 누적 경과에서 이어짐. 결과는 늘 RECORDING(PAUSED 는 TripStatus 에 없음).
 * 알 수 없는 값이면 null(첫 실행처럼).
 */
fun legacyRecordingToTrip(
    status: String,
    accumulatedMillis: Long,
    segmentStartMillis: Long,
    nowMillis: Long,
): CurrentTrip? {
    val accumulated = accumulatedMillis.coerceAtLeast(0L)
    val start = when (status) {
        TripStatus.RECORDING.name -> segmentStartMillis - accumulated
        Legacy.RECORDING_STATUS_PAUSED -> nowMillis - accumulated
        else -> return null
    }
    return CurrentTrip.start(start)
}
