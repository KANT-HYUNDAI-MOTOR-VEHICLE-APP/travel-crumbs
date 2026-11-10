package com.example.travelcrumbs.data.map

import com.example.travelcrumbs.domain.model.SidoNames
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 한국 지도 자산(#59) — 범위 파일 · 자산 못 읽음(M30). */
class KoreaMapRepositoryTest {

    private val boundsText = File("src/main/assets/maps/korea_bounds.json").readText()

    @Test
    fun `M06 시도 범위는 앱의 시도 목록 17개 모두 · 전국 범위 안`() {
        val asset = parseKoreaMapBounds(boundsText, "asset://maps/korea_style.json")
        assertEquals(SidoNames.ALL.toSet(), asset.sidoBounds.keys)
        asset.sidoBounds.values.forEach { bounds ->
            assertTrue(bounds.west >= asset.cameraBounds.west && bounds.east <= asset.cameraBounds.east)
            assertTrue(bounds.south >= asset.cameraBounds.south && bounds.north <= asset.cameraBounds.north)
        }
    }

    @Test
    fun `M31 스타일에 원격 주소 없음 · 지도 소스는 앱 자산`() {
        val style = File("src/main/assets/maps/korea_style.json").readText()
        assertTrue(style.contains("pmtiles://asset://maps/korea-z12.pmtiles"))
        assertTrue(!style.contains("http://") && !style.contains("https://"))
        assertTrue(!style.contains("\"glyphs\"") && !style.contains("\"sprite\""))
    }

    @Test
    fun `M30 지도 파일이 없으면 null(지도 못 그림)`() = runTest {
        val repository = AssetKoreaMapRepository(readText = { boundsText }, exists = { error("없음") })
        assertNull(repository.mapAsset())
    }

    @Test
    fun `M30 범위 파일이 깨지면 null`() = runTest {
        val repository = AssetKoreaMapRepository(readText = { "{\"country\": [1, 2]}" }, exists = { true })
        assertNull(repository.mapAsset())
    }

    @Test
    fun `자산이 다 있으면 스타일 주소는 앱 자산`() = runTest {
        val repository = AssetKoreaMapRepository(readText = { boundsText }, exists = { true })
        val asset = repository.mapAsset()
        assertNotNull(asset)
        assertEquals("asset://maps/korea_style.json", asset!!.styleUri)
    }
}
