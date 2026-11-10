package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.Place
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 추천 안내(B-04 · B-05 · #37)를 성호님 길안내 입구([GuideRequestTracker]) 하나로(#51 U7) — 옛 NaviRouteGuidanceRequester 대신.
 * 입구에 장소를 넘기고 그 요청의 결과(시작 · 실패)를 기다림 — 별점 자격(#41)은 입구가 「시작됨」으로 바꿨을 때만.
 * 입구가 다른 장소를 요청 중이면 이 요청은 보내지 않고 실패(입구는 한 번에 하나).
 * 입구의 동작(한도 · 늦은 시작 대조 · 핀 남기기)은 그대로 씀 — 고치지 않음. 메인 스레드에서 부름(입구 규칙).
 */
class TrackerRouteGuidanceRequester @Inject constructor(
    private val tracker: GuideRequestTracker,
) : RouteGuidanceRequester {

    override suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult {
        val place = Place(
            id = PLACE_ID_PREFIX + destination.placeNo,
            name = destination.placeNm,
            address = destination.addr,
            latitude = destination.lat,
            longitude = destination.lng,
        )
        val before = tracker.state.value.seq
        tracker.guide(place)
        val now = tracker.state.value
        if (now is GuideRequestState.Requesting && now.place.id != place.id) return GuidanceRequestResult.Failed
        val result = tracker.state.first { state ->
            state.seq > before && when (state) {
                is GuideRequestState.Started -> state.place.id == place.id
                is GuideRequestState.Failed -> state.place.id == place.id
                else -> false
            }
        }
        return if (result is GuideRequestState.Started) GuidanceRequestResult.Started else GuidanceRequestResult.Failed
    }

    private companion object {
        /** 내비로만 가는 장소 id — 추천 장소 번호 앞에 붙여 「가 볼 장소」 id 와 겹치지 않게(옛 어댑터와 같은 글자). */
        const val PLACE_ID_PREFIX = "reco-"
    }
}
