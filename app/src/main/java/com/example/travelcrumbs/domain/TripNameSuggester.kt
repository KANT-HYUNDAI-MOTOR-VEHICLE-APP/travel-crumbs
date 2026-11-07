package com.example.travelcrumbs.domain

/**
 * 여행명 「수정」을 눌렀을 때 이름을 추천받는 자리(결정 로그 D11).
 * 지금은 기본값을 그대로 돌려줌 — Gleo AI 담당 팀원이 이 인터페이스의 구현만 바꿔 끼우면 됨(di/TripModule).
 */
fun interface TripNameSuggester {
    /** [current] = 화면에 지금 보이는 여행명. 추천을 못 하면 [current] 를 그대로 돌려줌. */
    suspend fun suggest(current: String): String
}
