package com.example.travelcrumbs.ui.recommendations

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.LocationConsent
import com.example.travelcrumbs.domain.model.LocationResultKind
import com.example.travelcrumbs.domain.model.MapFitBasis
import com.example.travelcrumbs.domain.model.MapStatus
import com.example.travelcrumbs.domain.model.RankingSort
import com.example.travelcrumbs.domain.model.RouteMap
import com.example.travelcrumbs.domain.model.SketchPoint
import com.example.travelcrumbs.ui.components.RatingDialogItem
import com.example.travelcrumbs.ui.components.UiMessage

/** 「추천」 칸 안의 화면(와이어프레임 v6) — 목적지 안에서만 오가고 시스템 뒤로로 한 단계씩 돌아감. */
enum class RecommendationPage {
    /** B-01 지역 고르기. */
    REGION,

    /** B-02 지역 로드맵 랭킹. */
    RANKING,

    /** B-03 태그별로 보기. */
    TAGS,

    /** B-04 추천 로드맵 상세(B-05 팝업은 이 위에 뜸). */
    DETAIL,
}

/** 「추천」 칸이 그릴 것(#37) — 불변 · 화면 문구는 strings.xml · 데이터 값(이름 · 태그명)은 저장소가 준 그대로. 좌표 없음. */
data class RecommendationsUiState(
    val page: RecommendationPage = RecommendationPage.REGION,
    /** 화면 맨 위 「시연용 데이터」 한 줄(모든 빌드). */
    val demoData: Boolean = false,

    // B-01
    val sidoNms: List<String> = emptyList(),
    val selectedSidoNm: String? = null,
    val sigunguCards: List<RegionCardItem> = emptyList(),
    /** B-01 지도(#59) — 좌표 없음 · 장소 번호 · 개수 · 결과 종류만(스펙 3-1). */
    val map: RecommendationMapUi = RecommendationMapUi(),

    // B-02
    val regionSidoNm: String = "",
    /** null = 시도 전체. */
    val regionSigunguNm: String? = null,
    val sort: RankingSort = RankingSort.RATING,
    /** null = 동행자 전체. */
    val companion: CompanionType? = null,
    val rankedTrips: List<TripCardItem> = emptyList(),

    // B-03
    val regionTripCnt: Int = 0,
    val tagCounts: List<TagCountItem> = emptyList(),
    val selectedTagNo: Long? = null,
    val taggedTrips: List<TripCardItem> = emptyList(),

    // B-04 · B-05
    val detail: TripDetailItem? = null,
    val confirm: GuidanceConfirmItem? = null,
    /** 「안내 시작」을 누른 뒤 답을 기다리는 중 — 버튼을 한 번만 받음. */
    val guidanceRequesting: Boolean = false,
    /** B-06 별점 팝업(#41) — B-04 에서 열었을 때만. */
    val rating: RatingDialogItem? = null,
    /** 안내 요청 결과 · 별점 저장 한 줄(요청함 · 시작 못 함 · 남겼어요) — 화면이 보여 준 뒤 지움. */
    val message: UiMessage? = null,
) {
    /** 시스템 뒤로를 이 칸이 받는지 — 팝업이 떠 있거나 B-01 이 아닐 때. 아니면 앱 틀이 받음(「기록」으로). */
    val handlesBack: Boolean
        get() = confirm != null || rating != null || page != RecommendationPage.REGION ||
            map.card != null || map.candidates != null

    /** 머리글 「뒤로」 버튼 — B-02 · B-03 · B-04(하위 화면)만. B-01 은 레일 첫 화면이라 없음(시스템 뒤로는 앱 틀이 받음). */
    val showsBackButton: Boolean get() = page != RecommendationPage.REGION

    /** 선택 태그의 이름(B-03 목록 제목). */
    val selectedTagNm: String? get() = tagCounts.firstOrNull { it.tagNo == selectedTagNo }?.tagNm

    /** 막대 길이 기준 — 가장 많이 붙은 태그 수. */
    val maxTagCnt: Int get() = tagCounts.maxOfOrNull { it.cnt } ?: 0
}

/**
 * B-01 지도 표시 정보(#59 · 스펙 3-1) — 좌표 · 지도 가운데 · 화면 범위 칸 없음.
 * 위치 칸(동의 · 사용 허용 · 결과 종류)은 #55(LOCATION_RECO) 문구 반영 뒤 채움 — 지금은 늘 동의 안 함 · 허용 아니오 · 없음.
 */
data class RecommendationMapUi(
    val fitBasis: MapFitBasis = MapFitBasis.COUNTRY,
    /** 첫 맞춤 1회를 마쳤거나 사람이 지도를 밀거나 시도를 누름 — 늦은 위치 응답이 지도를 덮어쓰지 않게 하는 기준. */
    val firstFitDone: Boolean = false,
    val status: MapStatus = MapStatus.PREPARING,
    val consent: LocationConsent = LocationConsent.NOT_AGREED,
    val locationAllowed: Boolean = false,
    val locationResult: LocationResultKind = LocationResultKind.NONE,
    /** 화면 범위 핀 결과 — 지도 그리는 쪽이 계산해 넘긴 장소 번호. */
    val singlePlaceNos: List<Long> = emptyList(),
    val clusters: List<List<Long>> = emptyList(),
    val inRangeCnt: Int = 0,
    /** 시도 칩을 누를 때마다 오름 — 지도 그리는 쪽이 [focusSidoNm] 이 다 들어오게 이동. */
    val focusSidoNm: String? = null,
    val focusSeq: Int = 0,
    /** 묶음 핀 후보 목록(리뷰 많은 순) — 카드가 열려 있어도 「뒤로」로 돌아올 목록. */
    val candidates: List<MapCandidateItem>? = null,
    val card: MapPlaceCardItem? = null,
) {
    /** 지도 위에 보일 선택 장소 번호(핀 강조). */
    val selectedPlaceNo: Long? get() = card?.placeNo
}

/** 묶음 핀 후보 하나(스펙 2-3). */
data class MapCandidateItem(
    val placeNo: Long,
    val placeNm: String,
    val roadmapCnt: Int,
)

/** 핀 선택 장소 카드(스펙 2-3) — 좌표 · 기록 시각 · 계정 ID 없음. */
data class MapPlaceCardItem(
    val placeNo: Long,
    val placeNm: String,
    val tagNms: List<String>,
    /** 이 장소를 쓰는 공개 로드맵(리뷰 많은 순). */
    val roadmaps: List<MapRoadmapItem>,
    val selectedTripNo: Long,
    /** 후보 목록에서 연 카드 — 「뒤로」면 후보 목록으로. */
    val fromCandidates: Boolean,
) {
    val selectedRoadmap: MapRoadmapItem get() = roadmaps.first { it.tripNo == selectedTripNo }
}

/** 장소 카드의 로드맵 후보 하나. */
data class MapRoadmapItem(
    val tripNo: Long,
    val tripNm: String,
    val nicknameNm: String,
    val ratingAvg: Double,
    val ratingCnt: Int,
)

/** B-01 시군구 카드 — 시군구가 null 이면 「전체」. */
data class RegionCardItem(
    val sigunguNm: String?,
    val tripCnt: Int,
)

/** B-02 · B-03 로드맵 카드. */
data class TripCardItem(
    val rank: Int,
    val tripNo: Long,
    val tripNm: String,
    val nicknameNm: String,
    val ratingAvg: Double,
    val ratingCnt: Int,
    val placeCnt: Int,
    val representativeTagNm: String?,
)

/** B-03 태그 막대 하나. */
data class TagCountItem(
    val tagNo: Long,
    val tagNm: String,
    val cnt: Int,
)

/** B-04 상세. */
data class TripDetailItem(
    val tripNo: Long,
    val tripNm: String,
    val nicknameNm: String,
    val ratingAvg: Double,
    val ratingCnt: Int,
    val companionTypes: List<CompanionType>,
    val placeCnt: Int,
    val sidoNm: String,
    val sigunguNm: String?,
    val places: List<PlaceItem>,
    /** 경로 그림 점 — 장소 순서대로 · 0 ~ 1 상대 위치. */
    val sketch: List<SketchPoint>,
    /** 안내를 요청한 로드맵인지(별점 자격 · #41) — 아니면 별점 버튼 없음. */
    val canRate: Boolean = false,
    /** 내가 준 별 수 — 있으면 「내 별점 ★n · 고치기」. */
    val myRatingScore: Int? = null,
    /** 시연용 도로 지도 — 앱에 묶은 지도가 이 로드맵을 덮을 때만 · null 이면 [sketch] 로 그림. */
    val routeMap: RouteMap? = null,
)

/** B-04 장소 하나. */
data class PlaceItem(
    val placeNo: Long,
    val placeOrd: Int,
    val placeNm: String,
    val tagNms: List<String>,
)

/** B-05 안내 확인 팝업. */
data class GuidanceConfirmItem(
    val placeNo: Long,
    val placeNm: String,
    val addr: String,
    val nicknameNm: String,
    val tripNm: String,
    /** 로드맵의 첫 장소인지(「첫 장소」 · 「n번째 장소」 문구). */
    val placeOrd: Int,
)
