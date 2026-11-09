package com.example.travelcrumbs.data.map

import com.example.travelcrumbs.data.recommendation.RecommendationSeed
import com.example.travelcrumbs.domain.RoadmapMapRules
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapLayer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시연용 오프라인 지도 묶음 읽기(M1 · M2 · M4) · 실제 에셋이 시드 로드맵을 덮는지(M9). */
class OfflineMapPackTest {

    private val sample = """
        {"areas": {
          "town": {
            "label": "강원 테스트시",
            "bounds": [128.0, 38.0, 128.1, 38.1],
            "source": "https://example.invalid",
            "paths": [
              ["major", [128.01, 38.01, 128.02, 38.02, 128.03, 38.03]],
              ["water", [128.04, 38.04, 128.05, 38.04, 128.05, 38.05, 128.04, 38.04]],
              ["tunnel", [128.01, 38.01, 128.02, 38.02]],
              ["local", [128.01, 38.01]]
            ],
            "routes": [
              {"from": "1001", "to": "1002", "points": [128.01, 38.01, 128.015, 38.02, 128.02, 38.03]},
              {"from": "a", "to": "1002", "points": [128.01, 38.01, 128.02, 38.03]}
            ]
          }
        }}
    """.trimIndent()

    @Test
    fun `M1 지도 묶음 읽기 — 지역 이름 · 범위 · 층별 선`() {
        val pack = parseOfflineMapPack(sample)

        val area = pack.areas.single()
        assertEquals("town", area.areaId)
        assertEquals("강원 테스트시", area.label)
        assertEquals(128.1, area.bounds.east, 0.0)
        assertEquals(listOf(MapCoord(128.01, 38.01), MapCoord(128.02, 38.02), MapCoord(128.03, 38.03)), area.layers.getValue(MapLayer.MAJOR).single())
        assertEquals(4, area.layers.getValue(MapLayer.WATER).single().size)
    }

    @Test
    fun `M2 모르는 층 · 점 하나짜리 선은 뺌`() {
        val area = parseOfflineMapPack(sample).areas.single()

        assertEquals(setOf(MapLayer.MAJOR, MapLayer.WATER), area.layers.keys)
    }

    @Test
    fun `M4 구간 읽기 — 장소 번호 사이 · 번호가 아닌 구간은 뺌`() {
        val area = parseOfflineMapPack(sample).areas.single()

        val route = area.routes.single()
        assertEquals(1001L, route.fromPlaceNo)
        assertEquals(1002L, route.toPlaceNo)
        assertEquals(3, route.points.size)
    }

    @Test
    fun `M9 앱 에셋 — 시드 로드맵 전부를 한 지역이 덮고 장소 사이 구간이 다 있음`() {
        val file = listOf("src/main/assets/maps/roadmaps.json", "app/src/main/assets/maps/roadmaps.json")
            .map(::File).first { it.exists() }
        val pack = parseOfflineMapPack(file.readText())

        RecommendationSeed.trips.forEach { trip ->
            val ordered = trip.places.sortedBy { it.placeOrd }
            val area = RoadmapMapRules.areaFor(pack, ordered.map { MapCoord(it.lng, it.lat) })
            assertNotNull("${trip.tripNm} 을 덮는 지역 없음", area)
            ordered.zipWithNext().forEach { (from, to) ->
                assertTrue(
                    "${trip.tripNm} ${from.placeNo} → ${to.placeNo} 구간 없음",
                    RoadmapMapRules.segment(area!!, from.placeNo, to.placeNo).size >= 2,
                )
            }
        }
    }
}
