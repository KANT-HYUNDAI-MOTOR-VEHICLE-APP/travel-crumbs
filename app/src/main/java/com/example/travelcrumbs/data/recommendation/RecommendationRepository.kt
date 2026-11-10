package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 추천 화면이 공개 로드맵에 닿는 유일한 길(#37). 지금은 앱 안 시연용 시드([RecommendationSeed] · 서버 API 전 임시 · 서버가 생기면 시드는 debug 로 옮김) ·
 * 서버 API(지역 · 별점 · 랭킹 — 표준 등록 뒤)가 생기면 이 인터페이스의 구현만 바꿈(di/RecommendationModule).
 */
interface RecommendationRepository {
    /** 시연용 데이터인지 — debug 표시 한 줄에만 씀. */
    val isDemoData: Boolean

    /** 시도 · 시군구 목록(지역 마스터 · 제안). */
    suspend fun sidoRegions(): List<SidoRegion>

    /** 그 시도의 공개 로드맵 전부(정렬 · 필터 · 집계는 도메인 계산). */
    suspend fun publicTrips(sidoNm: String): List<RecommendedTrip>

    /** 태그 마스터(tag_mst) — 별점 「좋았던 점」 칩(#41). */
    suspend fun tags(): List<Tag>
}

/** 시드 저장소 — 서버 API 전 임시 · 모든 빌드에서 「시연용 데이터」 표시. 서버가 생기면 debug source set 으로 옮김. */
@Singleton
class SeedRecommendationRepository @Inject constructor() : RecommendationRepository {

    override val isDemoData: Boolean = true

    override suspend fun sidoRegions(): List<SidoRegion> = RecommendationSeed.sidoRegions

    override suspend fun publicTrips(sidoNm: String): List<RecommendedTrip> =
        RecommendationSeed.trips.filter { it.region.sidoNm == sidoNm }

    override suspend fun tags(): List<Tag> = RecommendationSeed.tags
}
