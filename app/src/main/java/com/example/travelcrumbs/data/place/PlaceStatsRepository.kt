package com.example.travelcrumbs.data.place

import com.example.travelcrumbs.data.remote.NavigationStartRemoteDataSource
import com.example.travelcrumbs.domain.model.PlaceStat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 장소별 안내 건수 조회(스펙 navigation 4.6 · 5절) — 단일 출처는 서버(시드 여행 + 모든 계정의 기록).
 * 지금 부르는 곳은 debug 진단 카드 한 줄뿐(U41) · 추천 화면은 별도 이슈.
 */
interface PlaceStatsRepository {
    suspend fun topStats(limit: Int): Result<List<PlaceStat>>
}

@Singleton
class RemotePlaceStatsRepository @Inject constructor(
    private val remote: NavigationStartRemoteDataSource,
) : PlaceStatsRepository {
    override suspend fun topStats(limit: Int): Result<List<PlaceStat>> = remote.stats(limit)
}
