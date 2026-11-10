package com.example.travelcrumbs.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.home.components.GUIDE_FAILURE_TAG
import com.example.travelcrumbs.ui.home.components.GUIDE_MAP_TAG
import com.example.travelcrumbs.ui.home.components.GUIDE_SCREEN_TAG
import com.example.travelcrumbs.ui.shell.AppShellScreen
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #4 길안내 연결 화면(스펙 navigation 2.3) Compose 시험 — 창 2개 영역 1682 × 1210 dp(#6 D18).
 * 홈의 「가 볼 장소」 구역을 뺀 뒤(미리보기)엔 화면 입구가 없어 ViewModel 의 onGuide 로 직접 엶 — 연결 화면 규칙만 지킴.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class HomeGuideScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = compose.activity.getString(id)

    private lateinit var graph: ShellTestGraph

    private fun showHome(graph: ShellTestGraph) {
        this.graph = graph
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    HomeScreen(onOpenRecommendations = {}, showSdkCard = false)
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showShell(graph: ShellTestGraph) {
        this.graph = graph
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    AppShellScreen()
                }
            }
        }
        compose.waitForIdle()
    }

    /** 첫 고정 장소 「a」로 안내 요청 — 전용 화면이 열림(화면과 같은 ViewModel). */
    private fun guideFirst() {
        compose.runOnUiThread { ViewModelProvider(graph.owner)[HomeViewModel::class.java].onGuide("a") }
        compose.waitForIdle()
    }

    private fun failFirst(graph: ShellTestGraph, reason: RouteFailReason = RouteFailReason.TOO_FAR) {
        guideFirst()
        graph.navi.answerRoute(RouteRequestResult.Failed(reason))
        compose.waitForIdle()
    }

    private fun none(value: String) = compose.onAllNodesWithText(value).fetchSemanticsNodes().isEmpty()

    private fun noneTag(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()

    @Test
    fun `T116 여기로 안내 — 전용 화면(지도 박스 · 목적지 · 현재 위치 · 요청 중 · 닫기) · 홈 목록 안 보임`() {
        showHome(ShellTestGraph(pinned = listOf("a", "b")))
        guideFirst()

        compose.onNodeWithTag(GUIDE_SCREEN_TAG).assertIsDisplayed()
        compose.onNode(hasText("a 이름") and hasAnyAncestor(hasTestTag(GUIDE_MAP_TAG))).assertIsDisplayed()
        compose.onNode(hasText("a 주소") and hasAnyAncestor(hasTestTag(GUIDE_MAP_TAG))).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_destination)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_origin)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.places_guide_requesting)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_close)).assertIsDisplayed().assertIsEnabled()
        assertTrue(noneTag(TOP_AREA_TAG))
        assertTrue(noneTag(GUIDE_FAILURE_TAG))
        assertTrue(none(text(R.string.guide_dummy_notice)))
    }

    @Test
    fun `T117 시작 알림 — 상태 줄 「NaviHelper 전달 완료 · 차량 내비게이션 시작」 · 열린 채`() {
        val graph = ShellTestGraph(pinned = listOf("a"))
        showHome(graph)
        guideFirst()
        graph.navi.answerRoute(RouteRequestResult.Started)
        compose.waitForIdle()

        compose.onNodeWithText(text(R.string.guide_route_started)).assertIsDisplayed()
        compose.onNodeWithTag(GUIDE_SCREEN_TAG).assertIsDisplayed()
        assertTrue(noneTag(GUIDE_FAILURE_TAG))
    }

    @Test
    fun `T118 debug 실패 — 상태 줄 실패 문구 · 실패 박스 글자 · 다시 시도 · 더미 경로 활성`() {
        val graph = ShellTestGraph(pinned = listOf("a"))
        showHome(graph)
        failFirst(graph)

        val failed = compose.activity.getString(R.string.places_route_failed, text(R.string.places_reason_too_far))
        compose.onNodeWithText(failed).assertIsDisplayed()
        compose.onNodeWithTag(GUIDE_FAILURE_TAG).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_fallback_label)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_retry)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.guide_dummy_route)).assertIsEnabled()
    }

    @Test
    fun `T120 더미 경로 — 지도 박스에 시연용 표시 · 더미 경로 비활성 · 내비 요청 그대로`() {
        val graph = ShellTestGraph(pinned = listOf("a"))
        showHome(graph)
        failFirst(graph)

        compose.onNodeWithText(text(R.string.guide_dummy_route)).performClick()
        compose.waitForIdle()

        compose.onNode(hasText(text(R.string.guide_dummy_notice)) and hasAnyAncestor(hasTestTag(GUIDE_MAP_TAG)))
            .assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_dummy_route)).assertIsNotEnabled()
        assertEquals(1, graph.navi.routeRequests.size)
        assertTrue(graph.pins.recorded.isEmpty())
    }

    @Test
    fun `T121 release 흉내 — 전용 화면 · 다시 시도만 · 더미 경로 · 실패 박스 글자 없음`() {
        val graph = ShellTestGraph(pinned = listOf("a"), dummyRoute = false)
        showHome(graph)
        failFirst(graph)

        compose.onNodeWithTag(GUIDE_SCREEN_TAG).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_retry)).assertIsEnabled()
        assertTrue(none(text(R.string.guide_dummy_route)))
        assertTrue(none(text(R.string.guide_fallback_label)))
    }

    @Test
    fun `T122 T130 닫기 — 홈 목록 · 폴백 버튼 없음 · 다시 열림`() {
        val graph = ShellTestGraph(pinned = listOf("a"))
        showHome(graph)
        failFirst(graph)
        compose.onNodeWithText(text(R.string.guide_dummy_route)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(text(R.string.guide_close)).performClick()
        compose.waitForIdle()

        assertTrue(noneTag(GUIDE_SCREEN_TAG))
        compose.onNodeWithTag(TOP_AREA_TAG).assertIsDisplayed()
        assertTrue(none(text(R.string.guide_retry)))
        assertTrue(none(text(R.string.guide_dummy_route)))
        assertTrue(none(text(R.string.guide_dummy_notice)))

        guideFirst()
        compose.onNodeWithTag(GUIDE_SCREEN_TAG).assertIsDisplayed()
    }

    @Test
    fun `T122 시스템 뒤로 — 전용 화면이 먼저 닫히고 Activity 는 그대로`() {
        val graph = ShellTestGraph(pinned = listOf("a", "b"))
        showHome(graph)
        guideFirst()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertTrue(noneTag(GUIDE_SCREEN_TAG))
        assertFalse(compose.activity.isFinishing)
    }

    @Test
    fun `T123 홈 목록에서 시스템 뒤로 — 홈이 막지 않음(#6 규칙 그대로 · Activity 로 넘어감)`() {
        showHome(ShellTestGraph(pinned = listOf("a")))

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertTrue(compose.activity.isFinishing)
    }

    @Test
    fun `T128 레일 여행 → 홈 · 홈 다시 터치 — 전용 화면 열린 채`() {
        val graph = ShellTestGraph(pinned = listOf("a"))
        showShell(graph)
        failFirst(graph)

        compose.onNodeWithText(text(R.string.nav_trips)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.nav_records)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(GUIDE_SCREEN_TAG).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.nav_records)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(GUIDE_SCREEN_TAG).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.guide_retry)).assertIsDisplayed()
    }

    @Test
    fun `T131 화면 점검 — 실패 · 더미 경로 켜짐에도 스크롤 없이 닫기 · 지도 · 상태 · 실패 박스가 한 화면 · 글 입력 없음 · 버튼 크기 E5 와 같음`() {
        val graph = ShellTestGraph(pinned = listOf("a"))
        showShell(graph)
        failFirst(graph)
        compose.onNodeWithText(text(R.string.guide_dummy_route)).performClick()
        compose.waitForIdle()

        val screen = compose.onNodeWithTag(GUIDE_SCREEN_TAG).getBoundsInRoot()
        val failure = compose.onNodeWithTag(GUIDE_FAILURE_TAG).getBoundsInRoot()
        val close = compose.onNodeWithText(text(R.string.guide_close)).getBoundsInRoot()
        assertTrue("실패 박스가 화면 안", failure.bottom <= screen.bottom)
        assertTrue("닫기가 화면 안", close.top >= screen.top)
        compose.onNodeWithText(text(R.string.nav_records)).assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        val scrollable = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy)
        assertTrue("전용 화면 안 스크롤 없음", compose.onAllNodes(scrollable and hasAnyAncestor(hasTestTag(GUIDE_SCREEN_TAG))).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodes(scrollable and hasTestTag(GUIDE_SCREEN_TAG)).fetchSemanticsNodes().isEmpty())
        val retry = compose.onNodeWithText(text(R.string.guide_retry)).getBoundsInRoot()
        assertTrue(retry.bottom - retry.top >= Dimens.HomeButtonHeight - Dimens.ItemSpacing)
    }
}
