package com.example.travelcrumbs.testing

import com.example.travelcrumbs.domain.GuidanceDestination
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RouteGuidanceRequester

/** 길안내 요청 포트 fake — 받은 목적지를 기록하고 [result] 로 답함 · [available] 거짓 = release(내비 연결 전). */
class FakeRouteGuidanceRequester(
    var result: GuidanceRequestResult = GuidanceRequestResult.Started,
    override var available: Boolean = true,
) : RouteGuidanceRequester {
    val requested = mutableListOf<GuidanceDestination>()

    override suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult {
        requested += destination
        return result
    }
}
