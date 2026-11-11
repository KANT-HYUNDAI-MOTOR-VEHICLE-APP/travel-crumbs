package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RoadmapRules
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 추천 = 시연용 시드 + 기기 안 내 공개 여행(#51 U7 · S-1 → S-2 「공개한 로드맵이 추천 재료」).
 * 내 공개 여행(COMPLETED · ANONYMOUS_PUBLIC)은 첫 장소 주소(모르면 A-01 목적지 주소)에서 지역(시도 · 시군구)을 얻어 그 지역 랭킹 · 태그별 보기에 바로 보임 ·
 * 작성자 = 지금 닉네임(바꾸면 다음 읽기부터 새 이름) · 번호 = [AppConfig.LOCAL_TRIP_RECO_NO_OFFSET] + trip_no(시드와 안 겹침).
 * 좌표가 없는 장소는 안내할 수 없어 빠지고, 남는 장소가 없거나 지역을 모르면 그 여행은 추천에 안 나옴.
 * 서버 API 가 생기면(ADR 0015) 이 구현 대신 서버 구현을 바인딩(di/RecommendationModule).
 */
@Singleton
class LocalRecommendationRepository @Inject constructor(
    private val trips: TripRepository,
    private val users: CurrentUserRepository,
    private val tagRepository: TagRepository,
    private val placeCatalog: PlaceCatalogRepository,
) : RecommendationRepository {

    override val isDemoData: Boolean = true

    override suspend fun sidoRegions(): List<SidoRegion> {
        val local = localTrips()
        val seed = RecommendationSeed.sidoRegions
        val extra = local.map { it.region }.filter { it.sigunguNm != null }
        val merged = seed.map { sido ->
            val more = extra.filter { it.sidoNm == sido.sidoNm }.mapNotNull { it.sigunguNm }.filter { it !in sido.sigunguNms }.distinct()
            sido.copy(sigunguNms = sido.sigunguNms + more)
        }
        val newSido = extra.map { it.sidoNm }.distinct().filter { name -> seed.none { it.sidoNm == name } }
            .map { name -> SidoRegion(name, extra.filter { it.sidoNm == name }.mapNotNull { it.sigunguNm }.distinct()) }
        return merged + newSido
    }

    override suspend fun publicTrips(sidoNm: String): List<RecommendedTrip> =
        RecommendationSeed.trips.filter { it.region.sidoNm == sidoNm } + localTrips().filter { it.region.sidoNm == sidoNm }

    override suspend fun allPublicTrips(): List<RecommendedTrip> = RecommendationSeed.trips + localTrips()

    override suspend fun tags(): List<Tag> = tagRepository.tags.first()

    private suspend fun localTrips(): List<RecommendedTrip> {
        val nickname = users.user.first().nicknameNm
        val tagByNo = tagRepository.tags.first().associateBy { it.tagNo }
        val destAddrs = placeCatalog.candidates().associate { it.name to it.address }
        return trips.trips.first()
            .filter { it.status == TripStatus.COMPLETED && it.visibility == Visibility.ANONYMOUS_PUBLIC }
            .mapNotNull { it.toRecommended(nickname, tagByNo, destAddrs[it.destPlaceNm]) }
    }

    private fun TripRecord.toRecommended(nickname: String, tagByNo: Map<Long, Tag>, destAddr: String?): RecommendedTrip? {
        val region = RoadmapRules.regionOfTrip(places, destAddr) ?: return null
        val located = places.filter { it.lat != null && it.lng != null }.sortedBy { it.placeOrd }
        if (located.isEmpty()) return null
        return RecommendedTrip(
            tripNo = AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + tripNo,
            tripNm = tripNm ?: destPlaceNm ?: region.sigunguNm ?: region.sidoNm,
            nicknameNm = nickname,
            region = region,
            companionTypes = companions,
            ratingAvg = 0.0,
            ratingCnt = 0,
            publishedAt = endAt ?: startAt,
            places = located.mapIndexed { index, place ->
                RecommendedPlace(
                    placeNo = AppConfig.LOCAL_TRIP_RECO_NO_OFFSET + place.tripPlaceNo,
                    placeNm = place.userPlaceNm ?: place.addr.orEmpty(),
                    placeOrd = index + 1,
                    addr = place.addr.orEmpty(),
                    lat = place.lat!!,
                    lng = place.lng!!,
                    tags = place.tagNos.mapNotNull { tagByNo[it] },
                )
            },
        )
    }
}
