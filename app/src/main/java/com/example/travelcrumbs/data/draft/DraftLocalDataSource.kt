package com.example.travelcrumbs.data.draft

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.model.Draft
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 임시 저장 한 건을 기기 안에 남김(스펙 app-shell D3). 내용은 로그에 찍지 않음. */
interface DraftLocalDataSource {
    val draft: Flow<Draft?>

    suspend fun save(draft: Draft)

    suspend fun clear()
}

@Singleton
class DataStoreDraftLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : DraftLocalDataSource {

    override val draft: Flow<Draft?> = dataStore.data.map { prefs ->
        prefs[AppPreferencesKeys.DRAFT_JSON]?.let { raw ->
            runCatching { json.decodeFromString(StoredDraft.serializer(), raw).toDomain() }
                .onFailure { Log.w(TAG, "임시 저장 읽기 실패: ${it.javaClass.simpleName}") }
                .getOrNull()
        }
    }

    override suspend fun save(draft: Draft) {
        val raw = json.encodeToString(StoredDraft.serializer(), StoredDraft.from(draft))
        dataStore.edit { it[AppPreferencesKeys.DRAFT_JSON] = raw }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(AppPreferencesKeys.DRAFT_JSON) }
    }

    @Serializable
    private data class StoredDraft(
        val destinationId: String,
        val fields: Map<String, String>,
        val savedAtMillis: Long,
    ) {
        fun toDomain() = Draft(destinationId, fields, savedAtMillis)

        companion object {
            fun from(draft: Draft) = StoredDraft(draft.destinationId, draft.fields, draft.savedAtMillis)
        }
    }

    private companion object {
        const val TAG = "DraftLocalDataSource"
    }
}
