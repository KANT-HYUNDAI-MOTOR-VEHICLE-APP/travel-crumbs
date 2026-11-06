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

    /** 장소 항목 오른쪽 버튼(「여기로 안내」 · 「고정」) 최소 폭 — 높이는 [HomeButtonHeight](스펙 navigation E5 · E10). */
    val PlaceButtonMinWidth = 220.dp

    /** 더미 경로 패널(스펙 navigation E13-2) 정지 도식 — 그림 높이 고정 · 점 반지름 · 선 굵기. */
    val DummyRouteDiagramHeight = 40.dp
    val DummyRouteDotRadius = 10.dp
    val DummyRouteLineWidth = 4.dp

    /** 「준비 중」 안내 한 줄 자리 — 늘 비워 두어 안내가 떠도 화면이 밀리지 않음. */
    val NoticeLineHeight = 40.dp
}
