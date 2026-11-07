package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviPlace
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceCandidatesTest {

    private fun place(name: String, lat: Double, lng: Double) = NaviPlace(name, null, lat, lng, "id-$name", "TMAP")

    @Test
    fun `300미터 안의 내비 장소만, 가까운 순`() {
        val market = place("강릉중앙시장", 37.7545, 128.8990) // 약 22m
        val parking = place("중앙시장 공영주차장", 37.7560, 128.8990) // 약 189m
        val beach = place("안목해변", 37.7720, 128.9470) // 약 4.6km

        val result = PlaceCandidates.near(37.7543, 128.8990, listOf(parking, beach, market))

        assertEquals(listOf(market, parking), result)
    }
}
