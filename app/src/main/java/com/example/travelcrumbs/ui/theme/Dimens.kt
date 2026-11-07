package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.unit.dp

/** 화면 크기 수치 한 곳 — dp 기준 수치화는 다음 이슈(스펙 app-shell D8). */
object Dimens {
    val ScreenPadding = 24.dp
    val ItemSpacing = 16.dp

    /** 주행 화면 버튼 — 크게 · 한 번 터치. */
    val DrivingButtonHeight = 96.dp

    /** 이 폭보다 좁으면 주행 화면 버튼을 세로로 쌓음. */
    val DrivingButtonsRowMinWidth = 640.dp

    /** 홈 맨 위 영역 버튼. */
    val HomeButtonHeight = 72.dp

    /** 터치 대상 최소 크기 — Android 접근성 권장 48 × 48dp(스펙 app-shell-v5 2-1 · V46). */
    val MinTouchTarget = 48.dp

    /** 레일 칸 높이 — 주행 중에도 누르는 칸이라 최소(48dp)보다 크게 · 5칸이 창 2개 영역 높이에 넉넉히 들어감. */
    val RailItemHeight = 72.dp

    /** 주행 화면 지도 자리 테두리. */
    val MapPlaceholderBorder = 2.dp

    /** 「준비 중」 안내 한 줄 자리 — 늘 비워 두어 안내가 떠도 화면이 밀리지 않음. */
    val NoticeLineHeight = 40.dp
}
