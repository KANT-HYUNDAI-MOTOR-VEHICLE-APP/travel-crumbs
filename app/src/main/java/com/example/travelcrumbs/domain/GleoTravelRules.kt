package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SidoNames
import com.example.travelcrumbs.domain.model.TripRecord
import java.time.Instant
import java.time.ZoneId

/**
 * Gleo 정보 조회(#69 SHOW_TRAVEL_INFO)의 순수 규칙 — 기기 없이 시험함.
 * 지역 맞추기(U3: 말한 지역 우선) · 가장 가까운 지역 · 오늘 기록 수.
 */
object GleoTravelRules {

    /** 긴 시도 이름 → 시드가 쓰는 짧은 이름(SidoNames). */
    private val SIDO_ALIASES: Map<String, String> = mapOf(
        "서울특별시" to "서울", "부산광역시" to "부산", "대구광역시" to "대구", "인천광역시" to "인천",
        "광주광역시" to "광주", "대전광역시" to "대전", "울산광역시" to "울산", "세종특별자치시" to "세종",
        "경기도" to "경기", "강원도" to "강원", "강원특별자치도" to "강원",
        "충청북도" to "충북", "충청남도" to "충남", "전라북도" to "전북", "전북특별자치도" to "전북",
        "전라남도" to "전남", "경상북도" to "경북", "경상남도" to "경남", "제주도" to "제주", "제주특별자치도" to "제주",
    )
    private val SUFFIX = Regex("(특별자치시|특별자치도|특별시|광역시|시|군|구|도)$")

    /** 비교용 이름 — 공백 제거 · 긴 시도 이름 줄임 · 「시 · 군 · 구 · 도」 꼬리 떼기(한 글자만 남으면 그대로). */
    fun normalize(name: String): String {
        val trimmed = name.trim().replace(" ", "")
        SIDO_ALIASES[trimmed]?.let { return it }
        val cut = trimmed.replace(SUFFIX, "")
        return if (cut.isEmpty()) trimmed else cut
    }

    /**
     * 사용자가 말한 지역([query])을 추천 로드맵의 지역들에 맞춤(U3).
     * - 말 전체가 시도 하나면 그 시도 전체(「제주」 → 제주시 + 서귀포시).
     * - 아니면 시군구 이름과 먼저 비교 · 함께 말한 시도가 있으면 그 시도 안에서만(「경북 경주」).
     * - 시군구로 안 맞고 시도가 맞으면 그 시도 전체.
     * 맞는 곳이 없으면 빈 목록 — 지어내거나 다른 지역으로 바꾸지 않음.
     */
    fun matchRegions(query: String, trips: List<RecommendedTrip>): List<Region> {
        val tokens = query.split(Regex("[\\s,·]+")).filter { it.isNotBlank() }.map(::normalize)
        if (tokens.isEmpty()) return emptyList()
        val regions = trips.map { it.region }.distinct()
        val sidos = regions.map { it.sidoNm }.distinct()
        val sidoHint = tokens.firstOrNull { it in SidoNames.ALL }
        if (tokens.size == 1 && sidoHint != null && sidoHint in sidos) return listOf(Region(sidoHint, null))
        val sigungu = regions.filter { region ->
            val name = region.sigunguNm ?: return@filter false
            normalize(name) in tokens && (sidoHint == null || region.sidoNm == sidoHint)
        }
        if (sigungu.isNotEmpty()) return sigungu
        return if (sidoHint != null && sidoHint in sidos) listOf(Region(sidoHint, null)) else emptyList()
    }

    /** 지역 하나(시군구 null = 시도 전체)에 드는 로드맵. */
    fun inAny(trips: List<RecommendedTrip>, regions: List<Region>): List<RecommendedTrip> = trips.filter { trip ->
        regions.any { it.sidoNm == trip.region.sidoNm && (it.sigunguNm == null || it.sigunguNm == trip.region.sigunguNm) }
    }

    /** 현재 위치에서 첫 장소가 가장 가까운 로드맵의 지역 — 위치가 없거나 로드맵이 없으면 null. */
    fun nearestRegion(location: CurrentLocation?, trips: List<RecommendedTrip>): Region? {
        location ?: return null
        return trips.mapNotNull { trip ->
            val first = trip.places.minByOrNull { it.placeOrd } ?: return@mapNotNull null
            trip.region to distanceMeters(location.lat, location.lng, first.lat, first.lng)
        }.minByOrNull { it.second }?.first
    }

    /** 오늘(기기 시간대 자정 이후) 기록한 장소 수 — 모든 여행 합계. */
    fun todayPlaceCount(trips: List<TripRecord>, nowMillis: Long, zone: ZoneId): Int {
        val startOfDay = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        return trips.sumOf { trip -> trip.places.count { it.recordedAt in startOfDay..nowMillis } }
    }

    /** 화면 · 콜백에 쓰는 지역 이름(「경북 경주시」 · 「제주」). */
    fun label(region: Region): String = listOfNotNull(region.sidoNm, region.sigunguNm).joinToString(" ")
}
