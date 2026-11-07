package com.example.travelcrumbs.domain.model

/** 버튼 연결 지점(「이 장소 기록」 · 「음성」)의 결과. */
enum class ActionResult {
    DONE,

    /** 아직 기능이 없음 → 화면이 「준비 중」 안내. */
    NOT_READY,
    FAILED,
}
