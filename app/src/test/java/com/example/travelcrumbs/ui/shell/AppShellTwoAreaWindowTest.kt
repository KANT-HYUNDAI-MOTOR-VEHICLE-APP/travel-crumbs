package com.example.travelcrumbs.ui.shell

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.home.HomeScreen
import com.example.travelcrumbs.ui.home.TOP_AREA_TAG
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.shell.components.railItemTag
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 앱 영역 두 크기(#51 U0 · Pleos Display Layout 「앱 영역은 앱 1~2개 · 가로 · 세로 배치 모두」) — 에뮬레이터 Pleos_Connect_v2.1.0 실측.
 * 넓은 칸 = 나누지 않은 앱 영역 1682 × 1210 dp(기본 설정) · 좁은 칸 = 가운데 ⋮ 로 나눈 한 칸 약 817 × 1210 dp(세로 · 내비가 옆 칸).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class AppShellTwoAreaWindowTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val graph = ShellTestGraph()

    private fun text(id: Int) = compose.activity.getString(id)

    private fun showShell() {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    AppShellScreen()
                }
            }
        }
    }

    private fun button(id: Int) = compose.onNode(hasText(text(id)) and hasClickAction())

    @Test
    fun `V29 창 2개 영역 주행 화면 — 레일 4칸 · 여행명 상태 경과 장소 수 · 버튼 4 · 팝업이 스크롤 없이 보이고 눌림`() {
        graph.driving.emit(DrivingState.DRIVING)
        showShell()

        AppDestination.railItems.forEach { compose.onNodeWithTag(railItemTag(it)).assertIsDisplayed() }
        compose.onNodeWithText(text(R.string.trip_name_default)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.driving_navi_card_title)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsDisplayed()
        button(R.string.action_voice).assertIsDisplayed()
        button(R.string.action_pause_recording).assertIsDisplayed()
        button(R.string.action_end_trip).assertIsDisplayed().performClick()

        compose.onNodeWithText(text(R.string.end_trip_confirm_body)).assertIsDisplayed()
        button(R.string.end_trip_confirm_continue).assertIsDisplayed()
        button(R.string.end_trip_confirm_end).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.trip_status_completed)).assertIsDisplayed()
    }

    @Test
    fun `V61 창 2개 영역(넓음) — 버튼 4개 한 줄 · 순서 그대로 · 48dp 이상 · 높이 72 · 간격 12 · 글자 5 mm 이상`() {
        graph.driving.emit(DrivingState.DRIVING)
        showShell()

        val bounds = assertDrivingButtons()
        assertTrue("한 줄", bounds.map { it.top }.distinct().size == 1)
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-land")
    fun `V61 좁은 창 — 버튼 4개 2 × 2 · 순서 그대로 · 48dp 이상 · 높이 72 · 간격 12 · 글자 5 mm 이상`() {
        graph.driving.emit(DrivingState.DRIVING)
        showShell()

        val bounds = assertDrivingButtons()
        assertTrue("2 × 2 — 윗줄 둘 · 화면 ${compose.onRoot().getBoundsInRoot()} · ${bounds}", bounds[0].top == bounds[1].top && bounds[2].top == bounds[3].top)
        assertTrue("2 × 2 — 아랫줄 간격 ${bounds[2].top - bounds[0].bottom} · 화면 ${compose.onRoot().getBoundsInRoot()}", bounds[2].top - bounds[0].bottom >= DESIGN_SYSTEM_SPACING)
    }

    @Test
    @Config(qualifiers = "w817dp-h1210dp-port")
    fun `U0-3 좁은 칸(나눈 칸 · 세로) 주행 — 레일 4칸 · 여행명 상태 경과 장소 수 · 내비 카드 없음 · 버튼 2 × 2 · 가로로 넘치지 않음`() {
        graph.driving.emit(DrivingState.DRIVING)
        showShell()

        AppDestination.railItems.forEach { compose.onNodeWithTag(railItemTag(it)).assertIsDisplayed() }
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasText(text(R.string.driving_navi_card_title))).fetchSemanticsNodes().isEmpty())
        val bounds = assertDrivingButtons()
        val root = compose.onRoot().getBoundsInRoot()
        assertTrue("2 × 2", bounds[0].top == bounds[1].top && bounds[2].top == bounds[3].top && bounds[2].top > bounds[0].top)
        bounds.forEach { assertTrue("가로로 넘치지 않음 $it · $root", it.right <= root.right) }
    }

    @Test
    @Config(qualifiers = "w817dp-h1210dp-port")
    fun `U0-4 좁은 칸(나눈 칸 · 세로) 정차 기록 칸 — 레일 4칸 · 진행 중 모양이 스크롤 없이 보이고 눌림`() {
        showShell()

        AppDestination.railItems.forEach { compose.onNodeWithTag(railItemTag(it)).assertIsDisplayed() }
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsDisplayed()
    }

    /** 버튼 4개(순서대로)의 경계 — 크기 · 간격 · 글자 높이를 재고 돌려줌. */
    private fun assertDrivingButtons(): List<DpRect> {
        val ids = listOf(
            R.string.action_record_place,
            R.string.action_voice,
            R.string.action_pause_recording,
            R.string.action_end_trip,
        )
        val bounds = ids.map { id ->
            val b = button(id).assertIsDisplayed().getBoundsInRoot()
            assertTrue("${text(id)} 폭 ${b.width}", b.width >= Dimens.MinTouchTarget)
            assertTrue("${text(id)} 높이 ${b.height} — 디자인 시스템 7절 72", b.height >= DESIGN_SYSTEM_BUTTON_HEIGHT)
            val label = compose.onNode(hasText(text(id)), useUnmergedTree = true).getBoundsInRoot()
            assertTrue("${text(id)} 글자 높이 ${label.height}", label.height >= FIVE_MM_AT_160_DPI)
            b
        }
        val sorted = bounds.sortedWith(compareBy({ it.top.value }, { it.left.value }))
        assertTrue("순서 그대로", sorted == bounds)
        bounds.zipWithNext().filter { (a, b) -> a.top == b.top }.forEach { (a, b) ->
            assertTrue("간격 ${b.left - a.right}", b.left - a.right >= DESIGN_SYSTEM_SPACING)
        }
        return bounds
    }

    @Test
    fun `V30 창 2개 영역 정차 기록 칸 — 진행 중 · 아님 두 모양이 스크롤 없이 보이고 눌림`() {
        showShell()

        AppDestination.railItems.forEach { compose.onNodeWithTag(railItemTag(it)).assertIsDisplayed() }
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()

        runBlocking { graph.recording.endTrip() }
        compose.waitForIdle()

        button(R.string.action_start_trip).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.recent_trip_empty)).assertIsDisplayed()
        button(R.string.action_open_recommendations).assertIsDisplayed().performClick()
        compose.onNode(hasText(text(R.string.nav_recommendations)) and isSelected()).assertIsDisplayed()
    }

    @Test
    fun `T37 V30 V68 창 2개 영역 기록 칸 순서 — 맨 위 영역 · 시험 구역 · debug 카드 · 맨 위 영역은 스크롤 없이 보임`() {
        val tallSection: @androidx.compose.runtime.Composable ColumnScope.() -> Unit = {
            Text(
                text = "시험 구역",
                modifier = Modifier
                    .testTag(SECTION_TAG)
                    .fillMaxWidth()
                    .height(1500.dp),
            )
        }
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    HomeScreen(onOpenRecommendations = {}, showSdkCard = true, featureSections = tallSection)
                }
            }
        }

        compose.onNodeWithTag(TOP_AREA_TAG).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_record_place)).assertIsDisplayed()

        val top = compose.onNodeWithTag(TOP_AREA_TAG).getBoundsInRoot()
        val section = compose.onNodeWithTag(SECTION_TAG).getBoundsInRoot()
        assertTrue("구역은 맨 위 영역 아래", section.top >= top.bottom)

        // 스크롤은 맨 위 영역 아래(구역 · 카드)만 — 같은 스크롤 위치에서 비교
        val card = compose.onNodeWithText(text(R.string.sdk_card_title)).performScrollTo().getBoundsInRoot()
        val scrolledSection = compose.onNodeWithTag(SECTION_TAG).getBoundsInRoot()
        assertTrue("debug 카드는 구역 아래", card.top >= scrolledSection.bottom)
    }

    @Test
    fun `T34 debug 카드를 끄면 기록 칸에 없음`() {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    HomeScreen(onOpenRecommendations = {}, showSdkCard = false)
                }
            }
        }

        compose.onAllNodes(hasText(text(R.string.sdk_card_title))).fetchSemanticsNodes().let {
            assertTrue(it.isEmpty())
        }
    }

    private companion object {
        const val SECTION_TAG = "test_section"

        /** 디자인 시스템 7절 — 주요 버튼 높이 72 · 인접 간격 12. */
        val DESIGN_SYSTEM_BUTTON_HEIGHT = 72.dp
        val DESIGN_SYSTEM_SPACING = 12.dp

        /** 5 mm · 160 dpi(에뮬레이터 Pleos_Connect_v2.1.0 · 1dp = 1px) ≈ 31.5dp. */
        val FIVE_MM_AT_160_DPI = (5f / 25.4f * 160f).dp
    }
}
