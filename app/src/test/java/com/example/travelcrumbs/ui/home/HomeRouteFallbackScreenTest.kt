package com.example.travelcrumbs.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.home.components.DUMMY_ROUTE_PANEL_TAG
import com.example.travelcrumbs.ui.home.components.PICKER_SECTION_TAG
import com.example.travelcrumbs.ui.home.components.PLACES_SECTION_TAG
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** #4 실패 폴백 Compose 시험(E11 ~ E13 · 창 2개 영역 = 앱 창 1682 × 1210 dp · #6 D18). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class HomeRouteFallbackScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = compose.activity.getString(id)

    private fun showHome(graph: ShellTestGraph) {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    HomeScreen(showSdkCard = false)
                }
            }
        }
        compose.waitForIdle()
    }

    /** 첫 「여기로 안내」(A) → 내비 오류 [reason]. */
    private fun failFirst(graph: ShellTestGraph, reason: RouteFailReason = RouteFailReason.TOO_FAR) {
        compose.onAllNodesWithText(text(R.string.places_guide))[0].performClick()
        compose.waitForIdle()
        graph.navi.answerRoute(RouteRequestResult.Failed(reason))
        compose.waitForIdle()
    }

    private fun inPanel(value: String) = hasText(value) and hasAnyAncestor(hasTestTag(DUMMY_ROUTE_PANEL_TAG))

    private fun assertNone(value: String) =
        assertTrue("$value 없음", compose.onAllNodesWithText(value).fetchSemanticsNodes().isEmpty())

    @Test
    fun `T97 실패 — 결과 문구 아래 다시 시도 · 더미 경로 둘 다 누를 수 있음 · 패널 없음`() {
        val graph = ShellTestGraph(pinned = listOf("a", "b"))
        showHome(graph)
        failFirst(graph)

        val failed = compose.activity.getString(R.string.places_route_failed, text(R.string.places_reason_too_far))
        compose.onNodeWithText(failed).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.places_retry)).assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText(text(R.string.places_dummy_route)).assertIsDisplayed().assertIsEnabled()
        assertTrue(compose.onAllNodes(hasTestTag(DUMMY_ROUTE_PANEL_TAG)).fetchSemanticsNodes().isEmpty())
        val resultLine = compose.onNodeWithText(failed).getBoundsInRoot()
        val retry = compose.onNodeWithText(text(R.string.places_retry)).getBoundsInRoot()
        assertTrue("폴백 버튼은 결과 문구 아래", retry.top >= resultLine.bottom)
    }

    @Test
    fun `T101 T102 더미 경로 → 패널(시연용 표시 · 현재 위치 · A 이름 · 주소 · 닫기) → 닫기`() {
        val graph = ShellTestGraph(pinned = listOf("a", "b"))
        showHome(graph)
        failFirst(graph)

        compose.onNodeWithText(text(R.string.places_dummy_route)).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(DUMMY_ROUTE_PANEL_TAG).assertIsDisplayed()
        compose.onNode(inPanel(text(R.string.dummy_route_notice))).assertIsDisplayed()
        compose.onNode(inPanel(text(R.string.dummy_route_origin))).assertIsDisplayed()
        compose.onNode(inPanel("a 이름")).assertIsDisplayed()
        compose.onNode(inPanel("a 주소")).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.places_dummy_route)).assertIsNotEnabled()
        assertEquals("내비 요청은 처음 1건뿐", 1, graph.navi.routeRequests.size)
        assertTrue(graph.pins.recorded.isEmpty())

        compose.onNode(inPanel(text(R.string.dummy_route_close))).performClick()
        compose.waitForIdle()

        assertTrue(compose.onAllNodes(hasTestTag(DUMMY_ROUTE_PANEL_TAG)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(text(R.string.places_retry)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.places_dummy_route)).assertIsEnabled()
    }

    @Test
    fun `T105 요청 중 — 여기로 안내 전부 비활성 · 폴백 없음 · 고정 · 고정 해제는 누를 수 있음(I-04)`() {
        val graph = ShellTestGraph(pinned = listOf("a", "b"))
        showHome(graph)
        failFirst(graph)
        compose.onNodeWithText(text(R.string.places_dummy_route)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(text(R.string.places_retry)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(text(R.string.places_guide_requesting)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.places_guide)).assertIsNotEnabled()
        assertNone(text(R.string.places_retry))
        assertNone(text(R.string.places_dummy_route))
        assertTrue(compose.onAllNodes(hasTestTag(DUMMY_ROUTE_PANEL_TAG)).fetchSemanticsNodes().isEmpty())
        compose.onAllNodesWithText(text(R.string.picker_pin))[0].assertIsEnabled()
        compose.onAllNodesWithText(text(R.string.picker_unpin))[0].assertIsEnabled()
    }

    @Test
    fun `T110 release 빌드 — 다시 시도만 · 더미 경로 없음`() {
        val graph = ShellTestGraph(pinned = listOf("a"), dummyRoute = false)
        showHome(graph)
        failFirst(graph)

        compose.onNodeWithText(text(R.string.places_retry)).assertIsDisplayed().assertIsEnabled()
        assertNone(text(R.string.places_dummy_route))
    }

    @Test
    fun `T111 패널 열림 — 맨 위 영역 그대로 · 패널은 가 볼 장소 안 · 폴백 줄 아래 · 장소 항목 위 · 글 입력 없음`() {
        val graph = ShellTestGraph(pinned = listOf("a", "b"))
        showHome(graph)
        val topBefore = compose.onNodeWithTag(TOP_AREA_TAG).getBoundsInRoot()
        failFirst(graph)
        compose.onNodeWithText(text(R.string.places_dummy_route)).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(TOP_AREA_TAG).assertIsDisplayed()
        assertEquals("맨 위 영역은 밀리지 않음", topBefore, compose.onNodeWithTag(TOP_AREA_TAG).getBoundsInRoot())
        val places = compose.onNodeWithTag(PLACES_SECTION_TAG).getBoundsInRoot()
        val panel = compose.onNodeWithTag(DUMMY_ROUTE_PANEL_TAG).getBoundsInRoot()
        val retry = compose.onNodeWithText(text(R.string.places_retry)).getBoundsInRoot()
        val firstPlace = compose.onNode(
            hasText("a 이름") and hasAnyAncestor(hasTestTag(PLACES_SECTION_TAG)) and !hasAnyAncestor(hasTestTag(DUMMY_ROUTE_PANEL_TAG)),
        ).getBoundsInRoot()
        val picker = compose.onNodeWithTag(PICKER_SECTION_TAG).getBoundsInRoot()
        assertTrue("패널은 가 볼 장소 구역 안", panel.top >= places.top && panel.bottom <= places.bottom)
        assertTrue("패널은 폴백 버튼 줄 아래", panel.top >= retry.bottom)
        assertTrue("패널은 고정 장소 항목 위", panel.bottom <= firstPlace.top)
        assertTrue("장소 고르기는 그 아래", picker.top >= places.bottom)
        assertTrue(compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
    }
}
