package com.example.travelcrumbs.data.local

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 기기 안 저장(DataStore Preferences)의 파일 이름 · 키 한 곳 — 지금 여행 요약 · 작성 중 내용(스펙 app-shell-v5 4-1) ·
 * 「가 볼 장소」 고정 목록(스펙 navigation U20) · 별점 기록(#41).
 */
object AppPreferencesKeys {
    const val FILE_NAME = "app_shell"

    /** 지금 여행 요약 — 상태 값은 TripStatus 영문 대문자 이름 그대로(표준 M01 결과가 바뀌면 여기와 읽기 한 곳만). */
    val TRIP_STATUS = stringPreferencesKey("trip_status")
    val TRIP_NAME = stringPreferencesKey("trip_name")
    val TRIP_START_AT_MILLIS = longPreferencesKey("trip_start_at_millis")
    val TRIP_END_AT_MILLIS = longPreferencesKey("trip_end_at_millis")
    val TRIP_PLACE_COUNT = intPreferencesKey("trip_place_count")

    /** 고정한 장소 id · 고정 순서(쉼표로 이음) — 이름 · 주소 · 좌표는 저장하지 않음. */
    val PINNED_PLACE_IDS = stringPreferencesKey("pinned_place_ids")

    /** 누적 기록 시간 · 지금 기록 구간 시작(v0.4 · 일시정지). #21 첫 판(PR #23) 저장 값엔 없음 — 읽기 변환은 RecordingLocalDataSource 한 곳(N10). */
    val TRIP_ACCUMULATED_MILLIS = longPreferencesKey("trip_accumulated_millis")
    val TRIP_SEGMENT_START_MILLIS = longPreferencesKey("trip_segment_start_millis")

    /** 작성 중 내용(D27 · 새 키). */
    val UNFINISHED_INPUT_JSON = stringPreferencesKey("unfinished_input_json")

    /** 별점 기록(#41) — 안내 요청한 로드맵 · 내 별점 · 자동 팝업을 띄운 로드맵(JSON 한 덩어리 · 읽기 변환은 RatingLocalDataSource 한 곳). */
    val RATING_RECORDS_JSON = stringPreferencesKey("rating_records_json")

    /**
     * 로드맵 검토(A-05 · A-06)를 끝낸(저장 · 나중에) 여행 번호들(쉼표로 이음 · #51 U5) — 끝난 최근 여행이 여기 없으면 검토 화면.
     * 서버 이행 때 trip 쪽(서버 여행 상태)으로 옮길 임시 자리.
     */
    val REVIEWED_TRIP_NOS = stringPreferencesKey("reviewed_trip_nos")

    /** #6 판 키 — 앱 업데이트 직후 한 번 옮기거나(기록 상태 · N10) 지우기만(작성 중 내용 · D27) 함. */
    object Legacy {
        val RECORDING_STATUS = stringPreferencesKey("recording_status")
        val RECORDING_ACCUMULATED_MILLIS = longPreferencesKey("recording_accumulated_millis")
        val RECORDING_SEGMENT_START_MILLIS = longPreferencesKey("recording_segment_start_millis")

        /**
         * #6 판 작성 중 내용 키 — 읽지 않고 지우기만 함(D27).
         * 글자는 #6 판 기기 안 파일에 남은 키 이름 그대로라 바꿀 수 없음(지우려면 같은 이름이어야 함).
         */
        val SHELL_INPUT_JSON = stringPreferencesKey("draft_json")
    }
}
