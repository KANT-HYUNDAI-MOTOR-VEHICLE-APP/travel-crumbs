package com.example.travelcrumbs.data.guidance

import com.example.travelcrumbs.domain.GuidanceDestination
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import javax.inject.Inject

/** release 빌드 — 차량 내비 길안내 연결(#4) 전이라 늘 실패로 답함 · 화면은 「안내를 시작할 수 없어요」. */
class UnavailableRouteGuidanceRequester @Inject constructor() : RouteGuidanceRequester {
    override suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult =
        GuidanceRequestResult.Failed
}
