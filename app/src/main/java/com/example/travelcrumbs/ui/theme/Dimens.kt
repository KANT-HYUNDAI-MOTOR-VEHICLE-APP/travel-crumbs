package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 화면 크기 수치 한 곳(색은 [TravelCrumbsColors] · 글자는 [TravelCrumbsTypography]). */
object Dimens {
    val ScreenPadding = 24.dp
    val ItemSpacing = 16.dp

    /** 주행 화면 버튼 — 크게 · 한 번 터치(디자인 시스템 7절 주요 버튼 높이 72 이상). */
    val DrivingButtonHeight = 96.dp

    /** 주행 화면 버튼 사이 간격(디자인 시스템 7절 인접 간격 12 이상). */
    val DrivingButtonSpacing = 16.dp

    /**
     * 이 폭보다 좁으면 주행 화면 버튼 4개를 2 × 2 로(D31). 가장 긴 글자 「기록 일시정지」(주요 버튼 28 · 약 175dp)
     * + 버튼 안쪽 여백 48 이 한 칸에 들어가는 폭(223) × 4 + 간격 16 × 3.
     */
    val DrivingButtonsRowMinWidth = 940.dp

    /** 기록 칸(정차) 맨 위 영역 버튼. */
    val HomeButtonHeight = 72.dp

    /** 터치 대상 최소 크기 — Android 접근성 권장 48 × 48dp(스펙 app-shell-v5 2-1 · V46). */
    val MinTouchTarget = 48.dp

    /**
     * 레일 — 글자 높이 5 mm 이상(Pleos UI 디자인 가이드 · 에뮬레이터 160 dpi 실측 기준 · #21 사람 결정).
     * 160 dpi 에서 1dp = 1px = 0.159 mm → 5 mm ≈ 31.5dp. 글자 줄 높이 [RailLabelLineHeight] 가 그 위.
     * 칸 높이 × 4 가 창 2개 영역 높이(1210dp) · 창 1개 영역 높이 안에 스크롤 없이 들어감.
     */
    val RailWidth = 120.dp
    val RailItemHeight = 96.dp

    /** 레일 아이콘 — 디자인 시스템 10절 32 이상(D38). */
    val RailIconSize = 32.dp

    /** 레일 가장자리와 칸 배경 사이 여백(선택 칸 배경 모서리는 [Corner]). */
    val RailItemInset = 8.dp
    val RailLabelFontSize = 24.sp
    val RailLabelLineHeight = 34.sp

    /** 버튼 · 카드 · 팝업 · 레일 선택 칸 모서리(디자인 시스템 7절 16 · Theme 의 Shapes). */
    val Corner = 16.dp

    /** 보조 · 위험 버튼 테두리. */
    val ButtonBorderWidth = 2.dp

    /** 주행 화면 지도 자리 테두리. */
    val MapPlaceholderBorder = 2.dp

    /** 「준비 중」 안내 한 줄 자리 — 늘 비워 두어 안내가 떠도 화면이 밀리지 않음. */
    val NoticeLineHeight = 40.dp
}
