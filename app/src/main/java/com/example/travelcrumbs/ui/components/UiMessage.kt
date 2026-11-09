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

    /** A-02 주행 중 「이 장소 기록」 — 위치만 임시저장(#51 U4). */
    PLACE_DRAFT_SAVED,

    /** A-03 「저장」 — 장소 이름 · 태그 · 메모 저장(#51 U4). */
    PLACE_SAVED,

    /** 「이 장소 기록」 — 위치를 못 읽음 · 장소는 위치 없이 저장(#51 U4). */
    LOCATION_UNAVAILABLE,

    /** A-01 — 길안내 · 내비를 열지 못함 · 기록은 시작(#51 U3). */
    NAVI_START_FAILED,

    /** A-06 · A-08 — 로드맵 저장 · 고침(#51 U5 · U6). */
    ROADMAP_SAVED,

    /** C-03 — 닉네임 저장(#51 U2). */
    NICKNAME_SAVED,

    /** C-03 — 위치정보 수집 동의 철회(#51 U2). */
    LOCATION_WITHDRAWN,

    /** C-03 — 추천 공개 동의 철회 · 공개 로드맵 모두 비공개(#51 U2). */
    RECO_WITHDRAWN,
}
