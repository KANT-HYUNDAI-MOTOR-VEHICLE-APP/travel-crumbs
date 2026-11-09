package com.example.travelcrumbs.ui.components

/** 화면이 보여 주고 지우는 한 줄 안내(스펙 app-shell 3-3 의 message). */
enum class UiMessage {
    NOT_READY,

    /** 추천 B-05 — 차량 내비에 길안내를 요청함(#37). */
    GUIDANCE_REQUESTED,

    /** 추천 B-05 — 길안내를 시작하지 못함(#37). */
    GUIDANCE_FAILED,

    /** 별점 B-06 — 내 별점을 남김(#41). */
    RATING_SAVED,
}
