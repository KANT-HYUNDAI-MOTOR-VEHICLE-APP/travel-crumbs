package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * 길안내 요청 하나의 끝을 기다림(스펙 navigation 3.2 전이표) — 먼저 오는 것으로 끝:
 * 시작 · 오류 알림([request]) / 내비 연결 끊김([connected] 가 거짓 → NOT_CONNECTED) /
 * 응답 대기 한도([timeoutMillis] → TIMEOUT) / 호출 예외(REQUEST_FAILED).
 */
suspend fun awaitRouteResult(
    connected: Flow<Boolean>,
    timeoutMillis: Long,
    request: suspend () -> RouteRequestResult,
): RouteRequestResult {
    val result = withTimeoutOrNull(timeoutMillis) {
        coroutineScope {
            val answer = async {
                try {
                    request()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    RouteRequestResult.Failed(RouteFailReason.REQUEST_FAILED)
                }
            }
            val lost = async {
                connected.first { !it }
                RouteRequestResult.Failed(RouteFailReason.NOT_CONNECTED)
            }
            select<RouteRequestResult> {
                answer.onAwait { it }
                lost.onAwait { it }
            }.also { coroutineContext.cancelChildren() }
        }
    }
    return result ?: RouteRequestResult.Failed(RouteFailReason.TIMEOUT)
}
