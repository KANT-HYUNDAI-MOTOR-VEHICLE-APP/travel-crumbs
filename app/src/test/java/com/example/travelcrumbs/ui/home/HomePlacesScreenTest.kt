package com.example.travelcrumbs.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 정차 홈 — #4 「가 볼 장소」 · 「장소 고르기」 구역을 홈에서 뺀 뒤(미리보기 · 사람 요청) Compose 시험
 * (Robolectric · 창 2개 영역 = 에뮬레이터 실측 앱 창 1682 × 1210 dp · #6 D18).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class HomePlacesScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = compose.activity.getString(id)

    private fun showHome(graph: ShellTestGraph, sdkCard: Boolean = false) {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    HomeScreen(onOpenRecommendations = {}, showSdkCard = sdkCard)
                }
            }
        }
        compose.waitForIdle()
    }

    /** 뺀 구역의 제목 · 버튼 문구(문자열 자원은 지웠으므로 글자 그대로). */
    private val removedTexts = listOf("개발용 · 길안내 시험(#4)", "가 볼 장소", "장소 고르기", "여기로 안내", "고정", "고정 해제")

    private fun assertNoPlaceSections() {
        removedTexts.forEach { removed ->
            assertTrue("$removed 없음", compose.onAllNodes(hasText(removed)).fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun `H1 여행 중 정차 — 가 볼 장소 · 장소 고르기 · 개발용 제목 없음 · 맨 위 영역 바로 아래 debug 카드`() {
        showHome(ShellTestGraph(pinned = listOf("a", "b", "c")), sdkCard = true)

        compose.onNodeWithTag(TOP_AREA_TAG).assertIsDisplayed()
        assertNoPlaceSections()
        val top = compose.onNodeWithTag(TOP_AREA_TAG).getBoundsInRoot()
        val card = compose.onNodeWithText(text(R.string.sdk_card_title)).performScrollTo().getBoundsInRoot()
        assertTrue("debug 카드는 맨 위 영역 아래", card.top >= top.bottom)
    }

    @Test
    fun `H2 여행 없음 정차 — 가 볼 장소 · 장소 고르기 · 개발용 제목 없음`() {
        showHome(ShellTestGraph(pinned = listOf("a"), withTrip = false), sdkCard = true)

        compose.onNodeWithTag(TOP_AREA_TAG).assertIsDisplayed()
        assertNoPlaceSections()
    }

    @Test
    fun `T26 정차 홈 — 글 입력 칸 없음`() {
        showHome(ShellTestGraph(pinned = listOf("a")))

        assertTrue(compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
    }
}
