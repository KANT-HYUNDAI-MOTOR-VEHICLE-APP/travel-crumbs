package com.example.travelcrumbs.ui.theme

import androidx.compose.ui.unit.dp

/** 화면 크기 수치 한 곳(색은 [TravelCrumbsColors] · 글자는 [TravelCrumbsTypography]). */
object Dimens {
    val ScreenPadding = 24.dp
    val ItemSpacing = 16.dp

    /** 섹션 사이 간격(디자인 시스템 7절 24) — 기록 칸의 맨 위 영역 · 「가 볼 장소」 · 「장소 고르기」 사이. */
    val SectionSpacing = 24.dp

    /** 주행 화면 버튼 — 크게 · 한 번 터치(디자인 시스템 7절 주요 버튼 높이 72 이상). */
    val DrivingButtonHeight = 96.dp

    /** 주행 화면 버튼 사이 간격(디자인 시스템 7절 인접 간격 12 이상). */
    val DrivingButtonSpacing = 16.dp

    /**
     * 이 폭보다 좁으면 주행 화면 버튼 4개를 2 × 2 로(D31). 가장 긴 글자 「기록 일시정지」(주요 버튼 28 · 약 175dp)
     * + 버튼 안쪽 여백 48 이 한 칸에 들어가는 폭(223) × 4 + 간격 16 × 3.
     */
    val DrivingButtonsRowMinWidth = 940.dp

    /** C-02 「여행 시작」 · 「추천 보러 가기」 카드 — 이 폭보다 좁으면(나눈 칸) 위아래로 · 카드 최소 높이(#51 U2). */
    val HomeChoiceRowMinWidth = 900.dp
    val HomeChoiceCardHeight = 160.dp

    /** 설정 카드 오른쪽 버튼(「수정」 · 「철회」) 최소 폭(#51 U2). */
    val SettingsActionMinWidth = 160.dp

    /** A-05 · A-08 — 이 폭 이상이면 경로 그림 왼쪽 · 장소 오른쪽 · 아니면 위아래(경로 그림 높이) · #51 U5. */
    val RoadmapWideMinWidth = 1000.dp
    val RoadmapSketchHeight = 320.dp

    /** 기록 칸(정차) 맨 위 영역 버튼. */
    val HomeButtonHeight = 72.dp

    /** 터치 대상 최소 크기 — Android 접근성 권장 48 × 48dp(스펙 app-shell-v5 2-1 · V46). */
    val MinTouchTarget = 48.dp

    /**
     * 레일 — 글자 높이 5 mm 이상(Pleos UI 디자인 가이드 · 에뮬레이터 160 dpi 실측 기준 · #21 사람 결정).
     * 160 dpi 에서 1dp = 1px = 0.159 mm → 5 mm ≈ 31.5dp. 글자는 본문 단계(Type.kt Body · 24 / 줄 높이 34).
     * 칸 높이 × 4 가 창 2개 영역 높이(1210dp) · 창 1개 영역 높이 안에 스크롤 없이 들어감.
     */
    val RailWidth = 120.dp
    val RailItemHeight = 96.dp

    /** 레일 아이콘 — 디자인 시스템 10절 32 이상(D38). */
    val RailIconSize = 32.dp

    /** 레일 가장자리와 칸 배경 사이 여백(선택 칸 배경 모서리는 [Corner]). */
    val RailItemInset = 8.dp

    /** 버튼 · 카드 · 팝업 · 레일 선택 칸 모서리(디자인 시스템 7절 16 · Theme 의 Shapes). */
    val Corner = 16.dp

    /** 보조 · 위험 버튼 테두리. */
    val ButtonBorderWidth = 2.dp

    /** 주행 화면 지도 자리 테두리. */
    /**
     * 주행 화면 내용 폭이 이 값 이상이면 「넓은 칸」(앱 영역 전체 · 내비가 옆에 없음) — 「내비 안내」 카드를 그림(#51 U0).
     * 나눈 칸(약 817dp − 레일 120)은 이보다 좁고, 나누지 않은 앱 영역(1682 − 120)은 넓음.
     */
    val DrivingWideMinWidth = 1000.dp
    /** 장소 항목 오른쪽 버튼(「여기로 안내」 · 「고정」) 최소 폭 — 높이는 [HomeButtonHeight](스펙 navigation E5 · E10). */
    val PlaceButtonMinWidth = 220.dp

    /** 길안내 연결 화면 지도 박스(스펙 navigation 2.3 G1 · G1-D) — 점 반지름 · 선 굵기 · 점이 모서리에서 떨어진 거리 · 글자 자리. */
    val GuideMapDotRadius = 10.dp
    val GuideMapLineWidth = 4.dp
    val GuideMapDotInset = 48.dp
    val GuideMapLabelOffset = 68.dp
    val GuideMapTextPadding = 160.dp

    /** 「준비 중」 안내 한 줄 자리 — 늘 비워 두어 안내가 떠도 화면이 밀리지 않음. */
    val NoticeLineHeight = 40.dp

    /** 추천(#37) 칩 · 작은 버튼 높이 — 터치 48 이상 + 본문 글자 한 줄. */
    val ChipHeight = 56.dp

    /** 추천 시군구 카드 높이(이름 + 로드맵 수 두 줄). */
    val RegionCardHeight = 120.dp

    /** 추천 시군구 카드 최소 폭 — 창 폭에 맞춰 줄마다 칸 수가 바뀜. */
    val RegionCardMinWidth = 220.dp

    /** 추천 로드맵 카드의 순위 칸 폭 · 경로 미리보기 칸. */
    val RankColumnWidth = 64.dp
    val TripThumbSize = 104.dp

    /** 추천 태그 막대 높이. */
    val TagBarHeight = 64.dp

    /** 추천 상세 경로 그림 — 점 지름 · 선 굵기 · 화면 폭 중 경로 칸 비율. */
    val SketchPinSize = 40.dp
    val SketchLineWidth = 4.dp
    const val DetailRouteWeight = 0.42f

    /** 추천 상세 도로 지도 — 지역 이름 · 출처 여백, 번호 핀 테두리, 지형 · 도로별 선 두께. */
    val RoadmapMapLabelPadding = 8.dp
    val RoadmapMapEdgePadding = 16.dp
    val RoadmapMapLabelInset = 44.dp
    val RoadmapMapPinOutline = 2.dp
    val RoadmapMapRiverWidth = 6.dp
    val RoadmapMapShoreWidth = 1.2.dp
    val RoadmapMapLocalRoadWidth = 0.8.dp
    val RoadmapMapStreetWidth = 1.3.dp
    val RoadmapMapMajorRoadWidth = 1.8.dp

    /** 추천 상세 장소 칸의 「여기로 안내」 버튼 최소 폭. */
    val GuideHereButtonMinWidth = 200.dp

    /**
     * 팝업 틀(AppDialog · 디자인 시스템 18절) 폭 = 창 폭 × [DialogWidthFraction] 을 [DialogMinWidth] ~ [DialogMaxWidth] 로 자름(#51 사람 요청).
     * 비율 0.6 = 사람이 실기기에서 「적당하다」고 본 B-05 팝업 폭(앱 창 1682dp 의 약 60%). 최소 560 = 버튼 두 개(주요 버튼 글자 28)
     * + 별 다섯 개(88 × 5 + 16 × 4) 가 한 줄에 드는 폭. 최대 1040 = 별점 팝업(#41) 이 쓰던 최대 폭 그대로.
     * 창이 최소보다 좁으면 창 폭(바깥 여백 뺀)까지 줄어듦.
     */
    const val DialogWidthFraction = 0.6f
    val DialogMinWidth = 560.dp
    val DialogMaxWidth = 1040.dp

    /** 별점 팝업(#41 B-06) 별 하나의 터치 칸. */
    val RatingStarSize = 88.dp

    /** 선택 카드 · 칩 테두리. */
    val SelectedBorderWidth = 2.dp
    val CardBorderWidth = 1.dp

    /** 카드(AppCard) 안 여백 · 안 요소 간격(디자인 시스템 7절 카드 내부 여백 20). */
    val CardPadding = 20.dp
    val CardContentSpacing = 8.dp

    /** 추천 지도(#59 B-01) — 낱개 핀 · 숫자 묶음 핀 지름(터치 칸 48dp 이상) · 테두리. */
    val RecoMapPinSize = 48.dp
    val RecoMapClusterSize = 64.dp
    val RecoMapPinBorder = 3.dp
    val RecoMapPinSelectedBorder = 6.dp

    /** 추천 지도 첫 맞춤 · 시도 이동 때 지도 가장자리 여백(짧은 변 비율). */
    const val RecoMapFitPaddingFraction = 0.06f

    /** 지역 고르기 머리(제목 · 칩 · 시군구) 최대 높이 — 남는 높이는 지도(창 높이 비율). */
    const val RecoRegionHeaderMaxFraction = 0.45f

    /** 지도 위 장소 카드 · 후보 목록 최대 폭 · 최대 높이(지도 높이 비율). */
    val RecoMapCardMaxWidth = 640.dp
    const val RecoMapCardMaxHeightFraction = 0.8f
}
