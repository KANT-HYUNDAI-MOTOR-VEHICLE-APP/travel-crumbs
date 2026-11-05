package com.example.travelcrumbs.data.place

import android.util.Log
import com.example.travelcrumbs.domain.AppConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/**
 * 「가 볼 장소」 고정 목록의 단일 출처(스펙 navigation 3.1 · 3.2 「고정 목록」).
 * 고정 · 해제는 핀(여행 기록)에 영향 없음.
 */
interface PinnedPlacesRepository {
    /** 저장된 고정 id · 고정 순서(후보에 없는 id 도 그대로 — 거르는 일은 후보와 합칠 때). */
    val pinnedIds: Flow<List<String>>

    /** 맨 아래에 붙임(U19) · 후보에 있는 고정 수가 최대(U16)면 그대로. 저장했으면 참. */
    suspend fun pin(placeId: String): Boolean

    /** 빼고 남은 순서 그대로. 저장했으면 참. */
    suspend fun unpin(placeId: String): Boolean
}

@Singleton
class DefaultPinnedPlacesRepository @Inject constructor(
    private val local: PinnedPlaceLocalDataSource,
    private val catalog: PlaceCatalogRepository,
) : PinnedPlacesRepository {

    override val pinnedIds: Flow<List<String>> = local.pinnedIds.distinctUntilChanged()

    override suspend fun pin(placeId: String): Boolean {
        val candidateIds = catalog.candidates().map { it.id }.toSet()
        if (placeId !in candidateIds) return false
        // 이미 고정 · 최대면 쓰기 없이 끝(T36 — 저장 쓰기 0) · 아래 transform 이 같은 조건을 원자적으로 한 번 더 봄
        val stored = try {
            local.pinnedIds.first()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w(TAG, "고정 목록 읽기 실패: ${error.javaClass.simpleName}")
            return false
        }
        if (placeId in stored || stored.count { it in candidateIds } >= AppConfig.PINNED_PLACES_MAX) return false
        return write { current ->
            val visible = current.count { it in candidateIds }
            if (placeId in current || visible >= AppConfig.PINNED_PLACES_MAX) current else current + placeId
        }
    }

    override suspend fun unpin(placeId: String): Boolean = write { current -> current - placeId }

    private suspend fun write(transform: (List<String>) -> List<String>): Boolean = try {
        local.update(transform)
        true
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        // 쓰기 실패 — 저장된 값 그대로 · 화면도 그대로 · 로그엔 이유 종류만(T47 · 4.4)
        Log.w(TAG, "고정 목록 저장 실패: ${error.javaClass.simpleName}")
        false
    }

    private companion object {
        const val TAG = "PinnedPlacesRepository"
    }
}
