package com.example.travelcrumbs.ui.demo

import com.example.travelcrumbs.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoExperienceTagsTest {
    private val destination = DemoGuidedDestination(
        "지현",
        Place("seoul-forest", "서울숲", "서울 성동구 뚝섬로 273", 37.5432, 127.0419),
    )

    @Test
    fun `같은 저장 장소의 여러 태그를 갱신하고 태그 없이 다시 저장하면 태그를 보존`() {
        val demo = DemoViewModel()
        demo.login(destination.account)
        demo.saveGuidedDestination(destination, listOf("산책하기 좋아요"))
        val updated = listOf("사진 찍기 좋아요", "한적해요")
        demo.saveGuidedDestination(destination, updated)
        demo.saveGuidedDestination(destination, null)

        val saved = demo.uiState.value.ownSavedPlaces.single()
        assertEquals(destination.place.name, saved.name)
        assertEquals(updated, saved.tags)
    }

    @Test
    fun `이미 기록한 장소의 태그를 갱신하면 로드맵에도 같은 한 곳이 남음`() {
        val demo = DemoViewModel()
        demo.login(destination.account)
        demo.setLocationConsent(true)
        demo.start()
        demo.addStop()
        val tags = listOf("산책하기 좋아요", "가족과 가기 좋아요")
        demo.saveGuidedDestination(destination, tags)
        demo.finish()

        val state = demo.uiState.value
        assertTrue(state.ownSavedPlaces.isEmpty())
        assertEquals(tags, state.ownTrip!!.stops.single().tags)
        assertEquals(tags, state.editedStops.single().tags)
    }
}
