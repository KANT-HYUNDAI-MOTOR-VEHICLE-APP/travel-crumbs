package com.example.travelcrumbs.data.local

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 기기 안 저장(DataStore Preferences)의 파일 이름 · 키 한 곳 — 기록 상태 · 임시 저장(스펙 app-shell D3) ·
 * 「가 볼 장소」 고정 목록(스펙 navigation U20).
 */
object AppPreferencesKeys {
    const val FILE_NAME = "app_shell"

    val RECORDING_STATUS = stringPreferencesKey("recording_status")
    val RECORDING_ACCUMULATED_MILLIS = longPreferencesKey("recording_accumulated_millis")
    val RECORDING_SEGMENT_START_MILLIS = longPreferencesKey("recording_segment_start_millis")

    val DRAFT_JSON = stringPreferencesKey("draft_json")

    /** 고정한 장소 id · 고정 순서(쉼표로 이음) — 이름 · 주소 · 좌표는 저장하지 않음. */
    val PINNED_PLACE_IDS = stringPreferencesKey("pinned_place_ids")
}
