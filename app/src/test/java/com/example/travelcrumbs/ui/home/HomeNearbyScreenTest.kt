package com.example.travelcrumbs.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.home.components.NEARBY_LIST_TAG
import com.example.travelcrumbs.ui.home.components.NEARBY_SECTION_TAG
import com.example.travelcrumbs.ui.home.components.nearbyCardTag
import com.example.travelcrumbs.ui.shell.AppShellScreen
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 기록 칸 「내 근처 리뷰 많은 로드맵」 화면(#55 · 스펙 home-nearby H20 · H28 ~ H31 · H33 · H40). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class HomeNearbyScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = compose.activity.getString(id)

    private fun graph(
        location: CurrentLocation? = null,
        driving: DrivingState = DrivingState.PARKED,
        nearbyConsent: Boolean = true,
    ): ShellTestGraph {
        val graph = ShellTestGraph(withTrip = false, initialDriving = driving)
        if (nearbyConsent) runBlocking { graph.users.recordConsent(ConsentType.LOCATION_RECO, true) }
        if (location != null) graph.navi.location = location
        return graph
    }

    private fun show(graph: ShellTestGraph) {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    AppShellScreen()
                }
            }
        }
        compose.waitForIdle()
    }

    private val nearbyCard = SemanticsMatcher("근처 로드맵 카드") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("home_nearby_card_") == true
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun `H1 H37 정차 · 속초 — 제목 · 설명 · 시연용 데이터 · 카드 3장 · 읽어 주기 한 묶음`() {
        show(graph())

        compose.onNodeWithText(text(R.string.home_nearby_title)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.home_nearby_desc, 10)).assertIsDisplayed()
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertIsDisplayed()
        listOf(101L, 102L, 103L).forEach { compose.onNodeWithTag(nearbyCardTag(it)).assertIsDisplayed() }
        compose.onNode(hasContentDescription("1위 · 속초 가족여행 · 강원 속초시 · 별점 4.8 · 리뷰 12명 · 여기서 0.1km"))
            .assertIsDisplayed()
    }

    @Test
    fun `H20 주행 — 주행 전용 화면 · 구역 없음`() {
        show(graph(driving = DrivingState.DRIVING))
        assertEquals(0, compose.onAllNodesWithTag(NEARBY_SECTION_TAG).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w817dp-h1210dp-port")
    fun `H28 H40 카드 10장(전국 대체) · 밀었다 놓으면 카드 한 장 단위로 맞춰 멈춤 · 겹침 없음 · 폭 고정`() {
        show(graph(location = CurrentLocation(33.0, 124.0, null)))
        compose.onNodeWithText(text(R.string.home_nearby_title_nationwide)).assertIsDisplayed()

        val list = compose.onNodeWithTag(NEARBY_LIST_TAG).fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag(NEARBY_LIST_TAG).performTouchInput { swipeLeft(startX = centerX, endX = centerX - width / 3) }
        compose.waitForIdle()

        // 잘리지 않은 자리 · 크기(목록 밖으로 나간 카드도 제 폭)
        val cards = compose.onAllNodes(nearbyCard).fetchSemanticsNodes()
            .map { Rect(it.positionInRoot, it.size.toSize()) }
            .sortedBy { it.left }
        assertTrue("밀어서 첫 카드가 왼쪽 밖으로 나감", cards.first().left < list.left)
        val leftmost = cards.first { it.right > list.left + 1f }
        assertTrue("카드 왼쪽 끝이 목록 왼쪽 끝에 맞음: ${leftmost.left} vs ${list.left}", abs(leftmost.left - list.left) < 2f)
        val widthPx = with(compose.density) { Dimens.NearbyCardWidth.toPx() }
        cards.forEach { assertEquals(widthPx, it.width, 1f) }
        cards.zipWithNext().forEach { (a, b) -> assertTrue("겹침 없음", b.left >= a.right) }
    }

    @Test
    fun `H29 H30 2위 카드 누름 — 추천 칸 · 레일 강조 추천 · 그 로드맵 B-04 · 시스템 뒤로 → 기록 칸`() {
        show(graph())

        compose.onNodeWithTag(nearbyCardTag(102L)).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.nav_recommendations)) and isSelected()).assertIsDisplayed()
        compose.onNodeWithText("바다 보고 시장 한 바퀴").assertIsDisplayed()
        compose.onNode(hasText(text(R.string.reco_guide_from_first)) and hasClickAction()).assertIsDisplayed()

        back()
        compose.onNode(hasText(text(R.string.nav_records)) and isSelected()).assertIsDisplayed()
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertIsDisplayed()
    }

    @Test
    fun `H30 머리 뒤로 → 기록 칸 · H33 레일 추천 다시 누름 → B-01`() {
        show(graph())

        compose.onNodeWithTag(nearbyCardTag(101L)).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.action_back)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertIsDisplayed()

        compose.onNodeWithTag(nearbyCardTag(101L)).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.nav_recommendations)) and hasClickAction()).performClick()
        compose.waitForIdle()
        // B-01 — 머리 「뒤로」 없음 · 「속초 가족여행」 상세가 아님
        assertEquals(0, compose.onAllNodes(hasText("속초 가족여행")).fetchSemanticsNodes().size)
    }

    @Test
    fun `H31 B-04 에서 B-05 팝업 — 뒤로 한 번은 팝업만 닫힘 · 한 번 더 → 기록 칸`() {
        show(graph())

        compose.onNodeWithTag(nearbyCardTag(101L)).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.reco_guide_from_first)) and hasClickAction()).performClick()
        compose.waitForIdle()
        back()
        compose.onNodeWithText("속초 가족여행").assertIsDisplayed()
        compose.onNode(hasText(text(R.string.nav_recommendations)) and isSelected()).assertIsDisplayed()
        back()
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertIsDisplayed()
    }

    @Test
    fun `H43 H44 동의 이력 없음 — 동의 카드 문구 · 버튼 둘 · 「동의하고 보기」 → 카드`() {
        val graph = graph(nearbyConsent = false)
        show(graph)

        compose.onNodeWithText(text(R.string.nearby_consent_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nearby_consent_body)).assertIsDisplayed()
        assertTrue(text(R.string.nearby_consent_body).lines().size == 4)
        compose.onNode(hasText(text(R.string.nearby_consent_decline)) and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_nearby_title)).assertDoesNotExist()

        compose.onNode(hasText(text(R.string.nearby_consent_agree)) and hasClickAction()).performClick()
        compose.waitForIdle()
        assertEquals(listOf(ConsentType.LOCATION_RECO to true), graph.users.history)
        compose.onNodeWithText(text(R.string.home_nearby_title)).assertIsDisplayed()
        compose.onNodeWithTag(nearbyCardTag(101L)).assertIsDisplayed()
    }

    @Test
    fun `H45 「괜찮아요」 — 구역 빠짐 · 거부 행`() {
        val graph = graph(nearbyConsent = false)
        show(graph)
        compose.onNode(hasText(text(R.string.nearby_consent_decline)) and hasClickAction()).performClick()
        compose.waitForIdle()
        assertEquals(listOf(ConsentType.LOCATION_RECO to false), graph.users.history)
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `H46 H51 설정 「철회」 → 기록 칸 구역 없음 · 설정 「동의」 팝업 「동의」 → 기록 칸 카드`() {
        val graph = graph()
        show(graph)
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertIsDisplayed()

        compose.onNode(hasText(text(R.string.nav_settings)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.settings_nearby_location)).assertIsDisplayed()
        compose.onNode(hasText(text(R.string.settings_withdraw)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.notice_nearby_location_withdrawn)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.settings_nearby_location_none)).assertIsDisplayed()

        compose.onNode(hasText(text(R.string.nav_records)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(NEARBY_SECTION_TAG).assertDoesNotExist()

        compose.onNode(hasText(text(R.string.nav_settings)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.settings_consent_agree)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.nearby_consent_body)).assertIsDisplayed()
        // 팝업의 「동의」(행 버튼과 같은 글자 — 팝업이 마지막에 그려짐)
        compose.onAllNodes(hasText(text(R.string.settings_consent_agree)) and hasClickAction()).fetchSemanticsNodes().let { nodes ->
            assertTrue(nodes.isNotEmpty())
        }
        compose.onAllNodes(hasText(text(R.string.settings_consent_agree)) and hasClickAction()).let { all ->
            all[all.fetchSemanticsNodes().size - 1].performClick()
        }
        compose.waitForIdle()
        assertEquals(ConsentType.LOCATION_RECO to true, graph.users.history.last())

        compose.onNode(hasText(text(R.string.nav_records)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(nearbyCardTag(101L)).assertIsDisplayed()
    }

    @Test
    fun `H52 설정 동의 팝업 「닫기」 — 팝업만 닫힘 · 이력 행 0`() {
        val graph = graph(nearbyConsent = false)
        show(graph)
        compose.onNode(hasText(text(R.string.nav_settings)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.settings_consent_agree)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNode(hasText(text(R.string.action_close)) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.nearby_consent_body)).assertDoesNotExist()
        assertTrue(graph.users.history.isEmpty())
    }
}
