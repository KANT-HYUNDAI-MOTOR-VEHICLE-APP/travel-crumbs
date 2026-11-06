package com.example.travelcrumbs.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.TEST_PLACES
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.testing.testPlace
import com.example.travelcrumbs.ui.home.components.PICKER_SECTION_TAG
import com.example.travelcrumbs.ui.home.components.PLACES_SECTION_TAG
import com.example.travelcrumbs.ui.shell.AppShellScreen
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 정차 홈 #4 구역 Compose 시험(Robolectric · 창 2개 영역 = 에뮬레이터 실측 앱 창 1682 × 1210 dp · #6 D18). */
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
                    HomeScreen(showSdkCard = sdkCard)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `T1 T70 순서 — 맨 위 영역 → 가 볼 장소 → 장소 고르기 → debug 카드 · 맨 위 영역은 스크롤 없이`() {
        showHome(ShellTestGraph(pinned = listOf("a", "b", "c")), sdkCard = true)

        compose.onNodeWithTag(TOP_AREA_TAG).assertIsDisplayed()
        val top = compose.onNodeWithTag(TOP_AREA_TAG).getBoundsInRoot()
        val places = compose.onNodeWithTag(PLACES_SECTION_TAG).getBoundsInRoot()
        val picker = compose.onNodeWithTag(PICKER_SECTION_TAG).getBoundsInRoot()
        assertTrue("가 볼 장소는 맨 위 영역 아래", places.top >= top.bottom)
        assertTrue("장소 고르기는 가 볼 장소 아래", picker.top >= places.bottom)
        val card = compose.onNodeWithText(text(R.string.sdk_card_title)).performScrollTo().getBoundsInRoot()
        val scrolledPicker = compose.onNodeWithTag(PICKER_SECTION_TAG).getBoundsInRoot()
        assertTrue("debug 카드는 장소 고르기 아래", card.top >= scrolledPicker.bottom)
    }

    @Test
    fun `T1 고정 3곳 — 이름 · 주소 · 여기로 안내 3개`() {
        showHome(ShellTestGraph(pinned = listOf("a", "b", "c")))

        compose.onNodeWithText(text(R.string.places_title)).assertIsDisplayed()
        assertEquals(3, compose.onAllNodesWithText(text(R.string.places_guide)).fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodesWithText(text(R.string.places_empty)).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText(text(R.string.places_navi_not_connected)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `T7 내비 연결 안 됨 — 연결 안 됨 문구 · 여기로 안내 전부 비활성`() {
        val graph = ShellTestGraph(pinned = listOf("a", "b"))
        graph.navi.setReady(false)
        showHome(graph)

        compose.onNodeWithText(text(R.string.places_navi_not_connected)).assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.places_guide)).fetchSemanticsNodes().indices.forEach { index ->
            compose.onAllNodesWithText(text(R.string.places_guide))[index].assertIsNotEnabled()
        }
    }

    @Test
    fun `T30 고정 0곳 — 제목 · 빈 안내 · 여기로 안내 없음 · 후보 전부 고정`() {
        showHome(ShellTestGraph())

        compose.onNodeWithText(text(R.string.places_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.places_empty)).assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText(text(R.string.places_guide)).fetchSemanticsNodes().isEmpty())
        assertEquals(TEST_PLACES.size, compose.onAllNodesWithText(text(R.string.picker_pin)).fetchSemanticsNodes().size)
    }

    @Test
    fun `T33 후보 고정 한 번 터치 — 가 볼 장소에 바로 나타남`() {
        showHome(ShellTestGraph())

        compose.onAllNodesWithText(text(R.string.picker_pin))[0].performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(1, compose.onAllNodesWithText(text(R.string.places_guide)).fetchSemanticsNodes().size)
        compose.onNodeWithText(text(R.string.picker_unpin)).assertIsDisplayed()
    }

    @Test
    fun `T35 고정 최대 — 한도 안내 · 고정 비활성 · 고정 해제는 누를 수 있음`() {
        showHome(ShellTestGraph(pinned = listOf("a", "b", "c")))

        compose.onNodeWithText(compose.activity.getString(R.string.picker_limit, AppConfig.PINNED_PLACES_MAX))
            .performScrollTo()
            .assertIsDisplayed()
        val pins = compose.onAllNodesWithText(text(R.string.picker_pin))
        assertEquals(2, pins.fetchSemanticsNodes().size)
        pins[0].assertIsNotEnabled()
        pins[1].assertIsNotEnabled()
        compose.onAllNodesWithText(text(R.string.picker_unpin))[0].assertIsEnabled()
    }

    @Test
    fun `T25 주소가 한 줄을 넘음 — 버튼 위치 · 크기 그대로`() {
        val long = testPlace("long", address = "아주 긴 주소 ".repeat(40))
        showHome(ShellTestGraph(pinned = listOf("long", "a"), places = listOf(long) + TEST_PLACES))

        val buttons = compose.onAllNodesWithText(text(R.string.places_guide))
        val longButton = buttons[0].getBoundsInRoot()
        val shortButton = buttons[1].getBoundsInRoot()
        assertEquals(shortButton.left, longButton.left)
        assertEquals(shortButton.right, longButton.right)
        assertTrue(longButton.right - longButton.left >= Dimens.PlaceButtonMinWidth)
    }

    @Test
    fun `T26 정차 홈 — 글 입력 칸 없음`() {
        showHome(ShellTestGraph(pinned = listOf("a")))

        assertTrue(compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `T19 T31 주행이면 앱 틀이 주행 화면만 — 가 볼 장소 · 장소 고르기 없음`() {
        val graph = ShellTestGraph(initialDriving = DrivingState.DRIVING, pinned = listOf("a", "b", "c"))
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    AppShellScreen()
                }
            }
        }
        compose.waitForIdle()

        assertTrue(compose.onAllNodes(hasText(text(R.string.places_title))).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodes(hasText(text(R.string.picker_title))).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodes(hasText(text(R.string.places_guide))).fetchSemanticsNodes().isEmpty())
    }
}
