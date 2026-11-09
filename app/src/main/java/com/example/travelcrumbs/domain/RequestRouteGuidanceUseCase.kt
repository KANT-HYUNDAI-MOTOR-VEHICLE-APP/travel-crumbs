package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.RecommendedPlace
import javax.inject.Inject

/** 추천 로드맵의 장소 하나로 차량 내비 길안내를 요청(#37 B-05) — 장소 → 안내 목적지로 바꿔 포트에 넘김. */
class RequestRouteGuidanceUseCase @Inject constructor(
    private val requester: RouteGuidanceRequester,
) {
    /** 실제 안내가 가는지(포트 그대로). */
    val available: Boolean get() = requester.available

    suspend operator fun invoke(place: RecommendedPlace): GuidanceRequestResult = requester.requestRoute(
        GuidanceDestination(
            placeNo = place.placeNo,
            placeNm = place.placeNm,
            addr = place.addr,
            lat = place.lat,
            lng = place.lng,
        ),
    )
}
