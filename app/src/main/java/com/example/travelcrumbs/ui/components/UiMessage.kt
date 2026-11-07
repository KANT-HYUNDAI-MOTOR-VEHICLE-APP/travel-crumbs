package com.example.travelcrumbs.ui.components

/** 화면이 보여 주고 지우는 한 줄 안내(스펙 app-shell 3-3 의 message). */
enum class UiMessage {
    NOT_READY,

    /** 로컬 서버(server/)에 연결하지 못함. */
    SERVER_UNAVAILABLE,

    /** 기록 중인 여행이 이미 있어 새 여행을 시작하지 않음(A-04 · 종료는 A-08). */
    TRIP_ALREADY_RECORDING,

    /** 여행 시작 저장 완료(A-04). */
    TRIP_STARTED,
}
