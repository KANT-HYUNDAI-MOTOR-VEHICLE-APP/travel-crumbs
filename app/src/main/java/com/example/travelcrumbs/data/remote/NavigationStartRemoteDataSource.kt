package com.example.travelcrumbs.data.remote

import com.example.travelcrumbs.data.auth.AccessTokenSource
import com.example.travelcrumbs.domain.model.PlaceStat
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** 핀 1건을 보낸 결과(스펙 navigation 4.6 표). */
enum class SendOutcome {
    /** 201 · 200 — 그 핀은 끝. */
    ACCEPTED,

    /** 422 — 다시 보내도 같은 결과 → 거절(다시 보내지 않음). */
    REJECTED,

    /** 토큰 없음 · 401 · 403 — 보내지 않고 대기(토큰 · 동의는 #1). 간격 재시도 대상 아님. */
    WAIT_FOR_AUTH,

    /** 네트워크 없음 · 시간 초과 · 5xx · 그 밖 — 대기 · 간격을 늘려 다시. */
    RETRY,
}

/** 서버 `navigation-starts` 를 부르는 유일한 곳 — 응답 코드를 [SendOutcome] 으로 바꿔 내보냄. */
@Singleton
class NavigationStartRemoteDataSource @Inject constructor(
    private val api: AppApi,
    private val tokens: AccessTokenSource,
) {
    suspend fun send(placeId: String, startedAtMillis: Long, utcOffsetSeconds: Int): SendOutcome {
        if (tokens.accessToken() == null) return SendOutcome.WAIT_FOR_AUTH
        val body = NavigationStartIn(placeId = placeId, startedAt = isoOffset(startedAtMillis, utcOffsetSeconds))
        val code = try {
            api.createNavigationStart(body).code()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: IOException) {
            return SendOutcome.RETRY
        } catch (_: RuntimeException) {
            return SendOutcome.RETRY
        }
        return when (code) {
            200, 201 -> SendOutcome.ACCEPTED
            401, 403 -> SendOutcome.WAIT_FOR_AUTH
            422 -> SendOutcome.REJECTED
            else -> SendOutcome.RETRY
        }
    }

    /** 장소별 안내 건수 — 실패는 HTTP 상태 · 예외 종류 코드로. */
    suspend fun stats(limit: Int): Result<List<PlaceStat>> {
        if (tokens.accessToken() == null) return Result.failure(IllegalStateException("NO_TOKEN"))
        return try {
            val response = api.getNavigationStats(limit)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.items.map { PlaceStat(it.placeId, it.startCount, it.userCount) })
            } else {
                Result.failure(IllegalStateException("HTTP_${response.code()}"))
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Result.failure(IllegalStateException(error.javaClass.simpleName))
        }
    }

    private fun isoOffset(epochMillis: Long, utcOffsetSeconds: Int): String =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.ofTotalSeconds(utcOffsetSeconds))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
