package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.MapArea
import com.example.travelcrumbs.domain.model.MapBounds
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapLayer
import com.example.travelcrumbs.domain.model.MapRoute
import com.example.travelcrumbs.domain.model.OfflineMapPack
import com.example.travelcrumbs.domain.model.RecommendedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 추천 상세 도로 지도 계산 — 지역 고르기(M3) · 덮지 못할 때 대체(M5) · 구간(M6 · M7) · 평면(M8). */
class RoadmapMapRulesTest {

    private val wide = area("wide", MapBounds(128.0, 38.0, 128.2, 38.2))
    private val town = area(
        "town",
        MapBounds(128.05, 38.05, 128.1, 38.1),
        routes = listOf(MapRoute(1, 2, listOf(MapCoord(128.06, 38.06), MapCoord(128.07, 38.065), MapCoord(128.08, 38.07)))),
    )
    private val pack = OfflineMapPack(listOf(wide, town))

    @Test
    fun `M3 지역 고르기 — 장소를 다 담는 지역 중 가장 좁은 것`() {
        assertEquals("town", RoadmapMapRules.areaFor(pack, listOf(MapCoord(128.06, 38.06), MapCoord(128.08, 38.07)))?.areaId)
        assertEquals("wide", RoadmapMapRules.areaFor(pack, listOf(MapCoord(128.06, 38.06), MapCoord(128.15, 38.15)))?.areaId)
    }

    @Test
    fun `M5 덮지 못하면 null — 지역 밖 장소 · 빈 장소 · 빈 묶음 (화면은 기존 경로 그림)`() {
        assertNull(RoadmapMapRules.areaFor(pack, listOf(MapCoord(128.06, 38.06), MapCoord(129.0, 38.06))))
        assertNull(RoadmapMapRules.areaFor(pack, emptyList()))
        assertNull(RoadmapMapRules.routeMap(OfflineMapPack(emptyList()), listOf(place(1, 1, 128.06, 38.06))))
        assertNull(RoadmapMapRules.routeMap(pack, listOf(place(1, 1, 128.06, 38.06), place(2, 2, 130.0, 38.07))))
    }

    @Test
    fun `M6 구간 — 정방향 그대로 · 반대 방향은 뒤집어 씀 · 없으면 빈 목록`() {
        assertEquals(MapCoord(128.06, 38.06), RoadmapMapRules.segment(town, 1, 2).first())
        assertEquals(MapCoord(128.08, 38.07), RoadmapMapRules.segment(town, 2, 1).first())
        assertTrue(RoadmapMapRules.segment(town, 1, 3).isEmpty())
    }

    @Test
    fun `M7 미리 계산한 구간이 없으면 두 장소를 곧게 이은 구간(onRoad 거짓)`() {
        val map = RoadmapMapRules.routeMap(
            pack,
            listOf(place(1, 1, 128.06, 38.06), place(2, 2, 128.08, 38.07), place(3, 3, 128.09, 38.09)),
        )!!

        assertEquals(listOf(true, false), map.legs.map { it.onRoad })
        assertEquals(3, map.legs[0].points.size)
        assertEquals(2, map.legs[1].points.size)
    }

    @Test
    fun `M8 평면 — 장소 순서대로 핀 · 북쪽이 위 · 동쪽이 오른쪽 · 범위 안 · 지역 이름`() {
        val map = RoadmapMapRules.routeMap(
            pack,
            // 순서가 섞여 들어와도 placeOrd 순
            listOf(place(2, 2, 128.08, 38.07), place(1, 1, 128.06, 38.06)),
        )!!

        assertEquals("town 지역", map.areaLabel)
        assertEquals(2, map.pins.size)
        val (first, second) = map.pins
        assertTrue("동쪽이 오른쪽", second.x > first.x)
        assertTrue("북쪽이 위", second.y < first.y)
        map.pins.forEach {
            assertTrue(kotlin.math.abs(it.x) <= map.halfWidth)
            assertTrue(kotlin.math.abs(it.y) <= map.halfHeight)
        }
        assertFalse(map.layers.getValue(MapLayer.MAJOR).isEmpty())
    }

    private fun area(id: String, bounds: MapBounds, routes: List<MapRoute> = emptyList()) = MapArea(
        areaId = id,
        label = "$id 지역",
        bounds = bounds,
        layers = mapOf(MapLayer.MAJOR to listOf(listOf(MapCoord(bounds.west, bounds.south), MapCoord(bounds.east, bounds.north)))),
        routes = routes,
    )

    private fun place(no: Long, ord: Int, lng: Double, lat: Double) = RecommendedPlace(
        placeNo = no,
        placeNm = "장소 $no",
        placeOrd = ord,
        addr = "",
        lat = lat,
        lng = lng,
        tags = emptyList(),
    )
}
