package com.example.travelcrumbs.data.place

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 고정 목록(장소 id · 고정 순서)을 기기 안에 남김 — DataStore(결정 기록 U20) · 이름 · 주소 · 좌표는 저장하지 않음. */
interface PinnedPlaceLocalDataSource {
    val pinnedIds: Flow<List<String>>

    /** 읽고 바꾸기를 한 번에 — 쓰기 실패는 예외로 알림(저장된 값은 그대로). */
    suspend fun update(transform: (List<String>) -> List<String>)
}

@Singleton
class DataStorePinnedPlaceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PinnedPlaceLocalDataSource {

    override val pinnedIds: Flow<List<String>> = dataStore.data.map { it.pinnedIds() }

    override suspend fun update(transform: (List<String>) -> List<String>) {
        dataStore.edit { prefs ->
            prefs[AppPreferencesKeys.PINNED_PLACE_IDS] = transform(prefs.pinnedIds()).joinToString(SEPARATOR)
        }
    }

    private fun Preferences.pinnedIds(): List<String> =
        this[AppPreferencesKeys.PINNED_PLACE_IDS].orEmpty().split(SEPARATOR).filter { it.isNotEmpty() }

    private companion object {
        /** 장소 id 꼴(영문 소문자 · 숫자 · `-`)에 없는 글자. */
        const val SEPARATOR = ","
    }
}
