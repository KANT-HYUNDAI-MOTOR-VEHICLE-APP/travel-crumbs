package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.MapBounds
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapMarker
import com.example.travelcrumbs.domain.model.MapPinResult
import com.example.travelcrumbs.domain.model.MapPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.ScreenPlace
import kotlin.math.cos

/** 추천 지도(#59)의 순수 계산 — 핀 원본 · 리뷰 수 순위(N12) · 로드맵 후보 순서 · 화면 묶음(N6) · 50개 한도. */
object RecommendationMapRules {

    /** 장소별 리뷰 수 = 그 장소를 쓰는 공개 로드맵들의 별점 참여 수 합(N12 ①) — 좌표 없는 값(ViewModel 정렬용). */
    fun placeReviewCnts(trips: List<RecommendedTrip>): Map<Long, Int> {
        val counts = mutableMapOf<Long, Int>()
        trips.forEach { trip ->
            trip.places.map { it.placeNo }.distinct().forEach { no -> counts[no] = (counts[no] ?: 0) + trip.ratingCnt }
        }
        return counts
    }

    /**
     * 지도 핀 원본 — 장소 번호마다 하나(같은 장소를 쓰는 여러 로드맵은 핀 하나 · 스펙 2-3).
     * 좌표가 숫자가 아니거나 (0, 0) 인 장소는 뺌(좌표 없는 장소 제외 · M10). 지도 그리는 쪽만 씀.
     */
    fun mapPlaces(trips: List<RecommendedTrip>): List<MapPlace> {
        val reviewCnts = placeReviewCnts(trips)
        return trips.flatMap { it.places }
            .filter { it.lat.isFinite() && it.lng.isFinite() && !(it.lat == 0.0 && it.lng == 0.0) }
            .distinctBy { it.placeNo }
            .map { MapPlace(it.placeNo, MapCoord(lng = it.lng, lat = it.lat), reviewCnts[it.placeNo] ?: 0) }
    }

    /** 그 장소를 쓰는 공개 로드맵 — 리뷰(별점 참여 수) 많은 순 · 같으면 로드맵 번호 순(M17). */
    fun roadmapsOfPlace(trips: List<RecommendedTrip>, placeNo: Long): List<RecommendedTrip> =
        trips.filter { trip -> trip.places.any { it.placeNo == placeNo } }
            .sortedWith(compareByDescending<RecommendedTrip> { it.ratingCnt }.thenBy { it.tripNo })

    /**
     * 가운데 [center] 에서 반경 [radiusKm] 가 다 들어오는 지도 범위(N3 첫 맞춤 10km) — 경도 폭은 위도에 따라 넓힘(cos 보정).
     * 지도 그리는 쪽만 씀(좌표 · 범위는 ViewModel 상태로 가지 않음).
     */
    fun fitBounds(center: MapCoord, radiusKm: Double): MapBounds {
        val latDelta = radiusKm / AppConfig.KM_PER_DEGREE_LAT
        val lngDelta = latDelta / cos(Math.toRadians(center.lat))
        return MapBounds(
            west = center.lng - lngDelta,
            south = center.lat - latDelta,
            east = center.lng + lngDelta,
            north = center.lat + latDelta,
        )
    }

    /** 장소 번호를 리뷰 많은 순 · 같으면 장소 번호 순으로(묶음 후보 목록 · M35). */
    fun rankPlaceNos(placeNos: Collection<Long>, reviewCnts: Map<Long, Int>): List<Long> =
        placeNos.distinct().sortedWith(compareByDescending<Long> { reviewCnts[it] ?: 0 }.thenBy { it })

    /**
     * 화면 범위 안 장소를 표시로 묶음(N6) — 리뷰 많은 장소부터 차례로 놓고, 이미 놓인 표시와 [radiusPx] 안이면 그 묶음에 넣음.
     * 새 표시는 [limit] 개까지 — 그 뒤 장소는 가까운 묶음이 있으면 거기에 더하고 없으면 그리지 않음(M36).
     */
    fun markers(places: List<ScreenPlace>, radiusPx: Float, limit: Int): List<MapMarker> {
        val ordered = places.sortedWith(compareByDescending<ScreenPlace> { it.reviewCnt }.thenBy { it.placeNo })
        val groups = mutableListOf<MutableList<ScreenPlace>>()
        val radius2 = radiusPx * radiusPx
        ordered.forEach { place ->
            val near = groups.firstOrNull { group ->
                val head = group.first()
                val dx = head.x - place.x
                val dy = head.y - place.y
                dx * dx + dy * dy <= radius2
            }
            when {
                near != null -> near.add(place)
                groups.size < limit -> groups.add(mutableListOf(place))
            }
        }
        return groups.map { group -> MapMarker(group.map { it.placeNo }, group.first().x, group.first().y) }
    }

    /** ViewModel 로 넘길 결과 — 장소 번호 · 개수만. */
    fun pinResult(markers: List<MapMarker>, inRangeCnt: Int): MapPinResult = MapPinResult(
        singlePlaceNos = markers.filterNot { it.isCluster }.map { it.placeNos.single() },
        clusters = markers.filter { it.isCluster }.map { it.placeNos },
        inRangeCnt = inRangeCnt,
    )
}
