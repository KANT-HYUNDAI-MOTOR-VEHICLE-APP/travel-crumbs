package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.map.KoreaMapRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import com.example.travelcrumbs.domain.model.KoreaMapAsset
import com.example.travelcrumbs.domain.model.MapBounds
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag

/** 한국 지도 자산 fake — 기본은 작은 범위 하나 · null 이면 「지도 못 그림」. */
class FakeKoreaMapRepository(
    var asset: KoreaMapAsset? = KoreaMapAsset(
        styleUri = "asset://maps/korea_style.json",
        countryBounds = MapBounds(124.6, 33.1, 131.9, 38.65),
        cameraBounds = MapBounds(124.0, 32.6, 132.4, 39.1),
        sidoBounds = mapOf("강원" to MapBounds(127.08, 37.02, 129.36, 38.62)),
    ),
) : KoreaMapRepository {
    override suspend fun mapAsset(): KoreaMapAsset? = asset
}

/** 시드 저장소를 감싸 공개 로드맵을 바꿀 수 있게 — 공개 철회 · 삭제(M26) 시험용. */
class MutableRecommendationRepository(
    private val seed: SeedRecommendationRepository = SeedRecommendationRepository(),
) : RecommendationRepository {
    var trips: List<RecommendedTrip>? = null
    var allLoads = 0
        private set

    override val isDemoData: Boolean = true

    override suspend fun sidoRegions(): List<SidoRegion> = seed.sidoRegions()

    override suspend fun publicTrips(sidoNm: String): List<RecommendedTrip> =
        (trips ?: seed.allPublicTrips()).filter { it.region.sidoNm == sidoNm }

    override suspend fun allPublicTrips(): List<RecommendedTrip> {
        allLoads++
        return trips ?: seed.allPublicTrips()
    }

    override suspend fun tags(): List<Tag> = seed.tags()
}
