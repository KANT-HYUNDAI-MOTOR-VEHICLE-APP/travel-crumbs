package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.unit.dp

/** 화면 크기 수치 한 곳 — 지시서 7장(간격과 형태) · 8장(화면 구조). 지시서 px = dp. */
object Dimens {
    /** 앱 영역 바깥 여백 · 섹션 간격. */
    val ScreenPadding = 24.dp
    val SectionSpacing = 24.dp

    /** 요소 간격(12 또는 16). */
    val ItemSpacing = 16.dp
    val ItemSpacingSmall = 12.dp

    /** 카드 내부 여백 · 모서리. */
    val CardPadding = 20.dp
    val CardCorner = 16.dp
    val ButtonCorner = 16.dp
    val BorderWidth = 2.dp

    /** 터치 영역 — 일반 64 · 주요 72 이상. */
    val TouchMin = 64.dp
    val PrimaryButtonHeight = 72.dp

    /** 아이콘 — 레일 32 이상 · 버튼 안 32. */
    val IconSize = 32.dp
    val RailIconSize = 36.dp

    /** 주행 화면 버튼 — 크게 · 한 번 터치. */
    val DrivingButtonHeight = 112.dp

    /** 이 폭보다 좁으면 주행 화면 버튼을 세로로 쌓음. */
    val DrivingButtonsRowMinWidth = 640.dp

    /** 홈과 길안내 동작 버튼은 공통 기본 버튼 높이를 씀. */
    val HomeButtonHeight = PrimaryButtonHeight

    /** 장소 항목 오른쪽 버튼(「여기로 안내」 · 「고정」) 최소 폭 — 높이는 [HomeButtonHeight](스펙 navigation E5 · E10). */
    val PlaceButtonMinWidth = 220.dp

    /** 길안내 연결 화면 지도 박스(스펙 navigation 2.3 G1 · G1-D) — 점 반지름 · 선 굵기 · 점이 모서리에서 떨어진 거리 · 글자 자리. */
    val GuideMapDotRadius = 10.dp
    val GuideMapLineWidth = 4.dp
    val GuideMapDotInset = 48.dp
    val GuideMapLabelOffset = 68.dp
    val GuideMapTextPadding = 160.dp

    /** 「준비 중」 안내 한 줄 자리 — 늘 비워 두어 안내가 떠도 화면이 밀리지 않음. */
    val NoticeLineHeight = 56.dp

    /** 화면 비율(8장) — 레일 12 · 지도 43 · 정보 45. */
    const val RailWeight = 12f
    const val MapWeight = 43f
    const val InfoWeight = 45f
}
