package com.example.travelcrumbs.data.unfinishedinput

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.model.UnfinishedInput
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 작성 중 내용 한 건을 기기 안에 남김(#6 D3 · 스펙 app-shell-v5 D27). 내용은 로그에 찍지 않음. */
interface UnfinishedInputLocalDataSource {
    /** 저장된 작성 중 내용 — 구독을 시작할 때 #6 판 옛 키 값은 읽지 않고 지움(D27 · V38). */
    val unfinishedInput: Flow<UnfinishedInput?>

    suspend fun save(input: UnfinishedInput)

    suspend fun clear()
}

@Singleton
class DataStoreUnfinishedInputLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : UnfinishedInputLocalDataSource {

    override val unfinishedInput: Flow<UnfinishedInput?> = dataStore.data
        .onStart { discardLegacy() }
        .map { prefs ->
            prefs[AppPreferencesKeys.UNFINISHED_INPUT_JSON]?.let { raw ->
                runCatching { json.decodeFromString(Stored.serializer(), raw).toDomain() }
                    .onFailure { Log.w(TAG, "작성 중 내용 읽기 실패: ${it.javaClass.simpleName}") }
                    .getOrNull()
            }
        }

    override suspend fun save(input: UnfinishedInput) {
        val raw = json.encodeToString(Stored.serializer(), Stored.from(input))
        dataStore.edit { it[AppPreferencesKeys.UNFINISHED_INPUT_JSON] = raw }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(AppPreferencesKeys.UNFINISHED_INPUT_JSON) }
    }

    /** 옛 키가 없으면 DataStore 가 파일을 다시 쓰지 않음(바뀐 것이 없으므로). */
    private suspend fun discardLegacy() {
        dataStore.edit { it.remove(AppPreferencesKeys.Legacy.SHELL_INPUT_JSON) }
    }

    @Serializable
    private data class Stored(
        val destinationId: String,
        val fields: Map<String, String>,
        val savedAtMillis: Long,
    ) {
        fun toDomain() = UnfinishedInput(destinationId, fields, savedAtMillis)

        companion object {
            fun from(input: UnfinishedInput) = Stored(input.destinationId, input.fields, input.savedAtMillis)
        }
    }

    private companion object {
        const val TAG = "UnfinishedInputLocal"
    }
}
