package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.MapArea
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapXY
import com.example.travelcrumbs.domain.model.OfflineMapPack
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RouteLeg
import com.example.travelcrumbs.domain.model.RouteMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

/**
 * 추천 상세(B-04)의 도로 지도 계산 — 순수 함수. 지역 고르기 · 구간 찾기 · 평면으로 옮기기.
 * 묶음이 로드맵을 덮지 못하면 null → 화면은 기존 경로 그림(점선)으로 대체.
 */
object RoadmapMapRules {

    /** 장소 전부를 담는 지역 중 가장 좁은 것. 장소가 없거나 담는 지역이 없으면 null. */
    fun areaFor(pack: OfflineMapPack, coords: List<MapCoord>): MapArea? {
        if (coords.isEmpty()) return null
        return pack.areas
            .filter { area -> coords.all(area.bounds::contains) }
            .minByOrNull { it.bounds.span }
    }

    /** 두 장소 사이 도로 구간 — 반대 방향만 있으면 뒤집어 씀 · 없으면 빈 목록. */
    fun segment(area: MapArea, fromPlaceNo: Long, toPlaceNo: Long): List<MapCoord> =
        area.routes.firstOrNull { it.fromPlaceNo == fromPlaceNo && it.toPlaceNo == toPlaceNo }?.points
            ?: area.routes.firstOrNull { it.fromPlaceNo == toPlaceNo && it.toPlaceNo == fromPlaceNo }?.points?.asReversed()
            ?: emptyList()

    /** 로드맵 장소(순서대로)를 덮는 지역이 있으면 그릴 지도, 없으면 null. */
    fun routeMap(pack: OfflineMapPack, places: List<RecommendedPlace>): RouteMap? {
        val ordered = places.sortedBy { it.placeOrd }
        val stops = ordered.map { MapCoord(it.lng, it.lat) }
        val area = areaFor(pack, stops) ?: return null

        val legCoords = ordered.zipWithNext().map { (from, to) ->
            val road = segment(area, from.placeNo, to.placeNo)
            if (road.size >= 2) road to true else listOf(MapCoord(from.lng, from.lat), MapCoord(to.lng, to.lat)) to false
        }
        val focus = stops + legCoords.flatMap { it.first }
        val centerLng = (focus.minOf { it.lng } + focus.maxOf { it.lng }) / 2
        val centerLat = (focus.minOf { it.lat } + focus.maxOf { it.lat }) / 2
        val lngFactor = cos(Math.toRadians(centerLat))

        fun project(coord: MapCoord) = MapXY(
            x = ((coord.lng - centerLng) * lngFactor).toFloat(),
            y = (centerLat - coord.lat).toFloat(),
        )

        val pins = stops.map(::project)
        val legs = legCoords.map { (coords, onRoad) -> RouteLeg(coords.map(::project), onRoad) }
        val projectedFocus = pins + legs.flatMap { it.points }
        return RouteMap(
            areaLabel = area.label,
            layers = area.layers.mapValues { (_, lines) -> lines.map { line -> line.map(::project) } },
            legs = legs,
            pins = pins,
            halfWidth = max(projectedFocus.maxOf { abs(it.x) }, MIN_HALF_EXTENT),
            halfHeight = max(projectedFocus.maxOf { abs(it.y) }, MIN_HALF_EXTENT),
        )
    }

    /** 장소가 한 곳 · 아주 가까울 때 지나치게 확대하지 않게 — 반폭 최소(도 단위 · 약 400m). */
    private const val MIN_HALF_EXTENT = 0.004f
}
