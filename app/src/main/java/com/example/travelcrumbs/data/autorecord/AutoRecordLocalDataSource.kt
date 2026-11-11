package com.example.travelcrumbs.data.autorecord

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.model.AutoPlaceName
import com.example.travelcrumbs.domain.model.PendingArrival
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 기록 대기 · 자동 이름 원본의 기기 안 저장(#66 스펙 4.2) — 프로세스 종료 · 시동 꺼짐 · 얼림을 넘김.
 * 값(이름 · 주소 · 좌표 · 도착 시각)은 로그에 찍지 않음 — 읽기 실패는 예외 종류만.
 */
interface AutoRecordLocalDataSource {
    suspend fun pendings(): List<PendingArrival>

    suspend fun savePendings(pendings: List<PendingArrival>)

    suspend fun names(): List<AutoPlaceName>

    suspend fun saveNames(names: List<AutoPlaceName>)

    /** 대기 · 원본 모두 지움. */
    suspend fun clearAll()
}

@Singleton
class DataStoreAutoRecordLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : AutoRecordLocalDataSource {

    override suspend fun pendings(): List<PendingArrival> =
        read(AppPreferencesKeys.AUTO_RECORD_PENDING_JSON, StoredPending.serializer())?.map { it.toDomain() }.orEmpty()

    override suspend fun savePendings(pendings: List<PendingArrival>) {
        write(AppPreferencesKeys.AUTO_RECORD_PENDING_JSON, StoredPending.serializer(), pendings.map(StoredPending::from))
    }

    override suspend fun names(): List<AutoPlaceName> =
        read(AppPreferencesKeys.AUTO_RECORD_NAMES_JSON, StoredName.serializer())?.map { AutoPlaceName(it.tripPlaceNo, it.name) }.orEmpty()

    override suspend fun saveNames(names: List<AutoPlaceName>) {
        write(AppPreferencesKeys.AUTO_RECORD_NAMES_JSON, StoredName.serializer(), names.map { StoredName(it.tripPlaceNo, it.name) })
    }

    override suspend fun clearAll() {
        dataStore.edit {
            it.remove(AppPreferencesKeys.AUTO_RECORD_PENDING_JSON)
            it.remove(AppPreferencesKeys.AUTO_RECORD_NAMES_JSON)
        }
    }

    private suspend fun <T> read(
        key: Preferences.Key<String>,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): List<T>? {
        val raw = dataStore.data.first()[key] ?: return null
        return runCatching { json.decodeFromString(ListSerializer(serializer), raw) }
            .onFailure { Log.w(TAG, "자동 기록 저장 읽기 실패: ${it.javaClass.simpleName}") }
            .getOrNull()
    }

    private suspend fun <T> write(
        key: Preferences.Key<String>,
        serializer: kotlinx.serialization.KSerializer<T>,
        items: List<T>,
    ) {
        dataStore.edit { prefs ->
            if (items.isEmpty()) prefs.remove(key) else prefs[key] = json.encodeToString(ListSerializer(serializer), items)
        }
    }

    @Serializable
    private data class StoredPending(
        val tripNo: Long,
        val name: String? = null,
        val address: String? = null,
        val latitude: Double,
        val longitude: Double,
        val arrivedAt: Long,
        val parkedSeen: Boolean,
    ) {
        fun toDomain() = PendingArrival(tripNo, name, address, latitude, longitude, arrivedAt, parkedSeen)

        companion object {
            fun from(p: PendingArrival) =
                StoredPending(p.tripNo, p.name, p.address, p.latitude, p.longitude, p.arrivedAt, p.parkedSeen)
        }
    }

    @Serializable
    private data class StoredName(val tripPlaceNo: Long, val name: String)

    private companion object {
        const val TAG = "AutoRecordLocal"
    }
}
