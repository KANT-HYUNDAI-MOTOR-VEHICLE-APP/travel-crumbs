package com.example.travelcrumbs.ui.recommendations

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.Tag

/** 지도 카드용 공개 메타데이터. 좌표 대신 미리 계산한 상세 그림과 장소 식별값만 보관. */
data class MapTripSnapshot(
    val detail: TripDetailItem,
    val publishedAt: Long,
    val places: List<MapPlaceMetadata>,
) {
    val tripNo: Long get() = detail.tripNo

    /** 기존 랭킹·카드 순수 계산용. 실제 좌표가 없으며 안내 요청에는 쓰지 않음. */
    fun metadataTrip() = RecommendedTrip(
        detail.tripNo, detail.tripNm, detail.nicknameNm, Region(detail.sidoNm, detail.sigunguNm),
        detail.companionTypes.toSet(), detail.ratingAvg, detail.ratingCnt, publishedAt,
        places.map { RecommendedPlace(it.no, it.name, it.order, it.address, 0.0, 0.0, it.tags) },
    )
}

data class MapPlaceMetadata(val no: Long, val name: String, val order: Int, val address: String, val tags: List<Tag>)
