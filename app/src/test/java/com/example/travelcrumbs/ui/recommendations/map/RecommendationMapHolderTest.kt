package com.example.travelcrumbs.ui.recommendations.map

import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.testing.FakeKoreaMapRepository
import com.example.travelcrumbs.testing.MutableRecommendationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 지도 렌더러 경계 보관 객체(#59 N16) — 카메라는 같은 진입 안 메모리만 · 핀 원본은 저장소에서 바로. */
class RecommendationMapHolderTest {

    private val repository = MutableRecommendationRepository()
    private val holder = RecommendationMapHolder(repository, FakeKoreaMapRepository())

    @Test
    fun `M38 같은 진입이면 카메라 그대로 · 새 진입이면 버리고 첫 맞춤`() {
        assertTrue(holder.beginEntry(1))
        holder.saveCamera(MapCamera(MapCoord(lng = 128.59, lat = 38.2), zoom = 12.0))
        holder.markFocusApplied(3)

        // B-02 · B-04 다녀옴(같은 진입 값)
        assertFalse(holder.beginEntry(1))
        assertNotNull(holder.camera)
        assertEquals(3, holder.appliedFocusSeq)

        // 레일 「추천」을 다시 누름(새 진입 값)
        assertTrue(holder.beginEntry(2))
        assertNull(holder.camera)
        assertEquals(0, holder.appliedFocusSeq)
    }

    @Test
    fun `M11 M26 핀 원본은 저장소의 공개 로드맵에서 매번 다시 읽음`() = runTest {
        val first = holder.places()
        assertTrue(first.any { it.placeNo == 1001L })

        repository.trips = emptyList()
        assertTrue(holder.places().isEmpty())
        assertEquals(2, repository.allLoads)
    }
}
