package com.example.travelcrumbs.data.local

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** 기기 안 저장(DataStore Preferences)의 파일 이름 · 키 한 곳 — 지금 여행 요약 · 작성 중 내용(스펙 app-shell-v5 4-1). */
object AppPreferencesKeys {
    const val FILE_NAME = "app_shell"

    /** 지금 여행 요약 — 상태 값은 TripStatus 영문 대문자 이름 그대로(표준 M01 결과가 바뀌면 여기와 읽기 한 곳만). */
    val TRIP_STATUS = stringPreferencesKey("trip_status")
    val TRIP_NAME = stringPreferencesKey("trip_name")
    val TRIP_START_AT_MILLIS = longPreferencesKey("trip_start_at_millis")
    val TRIP_END_AT_MILLIS = longPreferencesKey("trip_end_at_millis")
    val TRIP_PLACE_COUNT = intPreferencesKey("trip_place_count")

    /** 작성 중 내용(D27 · 새 키). */
    val UNFINISHED_INPUT_JSON = stringPreferencesKey("unfinished_input_json")

    /** #6 판 키 — 앱 업데이트 직후 한 번 옮기거나(기록 상태 · N10) 지우기만(작성 중 내용 · D27) 함. */
    object Legacy {
        val RECORDING_STATUS = stringPreferencesKey("recording_status")
        val RECORDING_ACCUMULATED_MILLIS = longPreferencesKey("recording_accumulated_millis")
        val RECORDING_SEGMENT_START_MILLIS = longPreferencesKey("recording_segment_start_millis")

        /** #6 판 기록 상태 값 중 일시정지 — TripStatus 에는 없는 값. */
        const val RECORDING_STATUS_PAUSED = "PAUSED"

        /**
         * #6 판 작성 중 내용 키 — 읽지 않고 지우기만 함(D27).
         * 글자는 #6 판 기기 안 파일에 남은 키 이름 그대로라 바꿀 수 없음(지우려면 같은 이름이어야 함).
         */
        val SHELL_INPUT_JSON = stringPreferencesKey("draft_json")
    }
}
