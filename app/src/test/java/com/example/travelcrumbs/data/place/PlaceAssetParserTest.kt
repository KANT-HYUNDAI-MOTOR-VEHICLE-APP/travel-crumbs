package com.example.travelcrumbs.data.place

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 후보 장소 JSON(스펙 navigation 4.1 · U1 · U3). */
class PlaceAssetParserTest {

    @Test
    fun `U1 assets 의 후보 JSON — 5곳 · 적힌 순서 · id 꼴이 계약과 같음`() {
        val text = File("src/main/assets/places.json").readText()

        val places = parsePlaces(text)

        assertEquals(
            listOf("42dot", "sw-dream-center", "gyeongbokgung", "namsan-tower", "haeundae-beach"),
            places.map { it.id },
        )
        assertTrue(places.all { it.name.isNotBlank() && it.address.isNotBlank() })
        assertTrue("후보 수 > 고정 최대", places.size > com.example.travelcrumbs.domain.AppConfig.PINNED_PLACES_MAX)
    }

    @Test
    fun `꼴이 틀린 id · 빈 이름 · 겹친 id 는 빠지고 나머지는 순서 그대로`() {
        val text = """
            {"places": [
              {"id": "ok-1", "name": "가", "address": "주소", "latitude": 37.0, "longitude": 127.0},
              {"id": "Bad_ID", "name": "나", "address": "주소", "latitude": 37.0, "longitude": 127.0},
              {"id": "ok-2", "name": " ", "address": "주소", "latitude": 37.0, "longitude": 127.0},
              {"id": "ok-1", "name": "다", "address": "주소", "latitude": 37.0, "longitude": 127.0},
              {"id": "ok-3", "name": "라", "address": "주소", "latitude": 37.0, "longitude": 127.0}
            ]}
        """.trimIndent()

        assertEquals(listOf("ok-1", "ok-3"), parsePlaces(text).map { it.id })
    }
}
