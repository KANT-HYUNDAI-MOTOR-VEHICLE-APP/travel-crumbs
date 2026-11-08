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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.home.HomeScreen
import com.example.travelcrumbs.ui.home.TOP_AREA_TAG
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.shell.components.railItemTag
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 창 2개 영역(landscape 전체 폭 · 스펙 app-shell D18 · app-shell-v5 V29 · V30) — 에뮬레이터 Pleos_Connect_v2.1.0 실측 앱 창 1682 × 1210 dp.
 * landscape 고정 앱은 창 1개 영역으로 줄지 않음(Pleos 가 「창 크기를 넓게」 안내 · 앱 책임 아님).
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
    fun `V29 창 2개 영역 주행 화면 — 레일 5칸 · 여행명 상태 경과 장소 수 · 버튼 3 · 팝업이 스크롤 없이 보이고 눌림`() {
        graph.driving.emit(DrivingState.DRIVING)
        showShell()

        AppDestination.railItems.forEach { compose.onNodeWithTag(railItemTag(it)).assertIsDisplayed() }
        compose.onNodeWithText(text(R.string.trip_name_default)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.map_placeholder)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsDisplayed()
        button(R.string.action_voice).assertIsDisplayed()
        button(R.string.action_end_trip).assertIsDisplayed().performClick()

        compose.onNodeWithText(text(R.string.end_trip_confirm_body)).assertIsDisplayed()
        button(R.string.end_trip_confirm_continue).assertIsDisplayed()
        button(R.string.end_trip_confirm_end).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.trip_status_completed)).assertIsDisplayed()
    }

    @Test
    fun `V30 창 2개 영역 정차 홈 — 레일 5칸 · 설정 · 새 여행 시작 · 기록 상태 · 이 장소 기록이 보이고 눌림`() {
        showShell()

        AppDestination.railItems.forEach { compose.onNodeWithTag(railItemTag(it)).assertIsDisplayed() }
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        button(R.string.action_start_new_trip).assertIsDisplayed()
        button(R.string.action_record_place).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        button(R.string.nav_settings).assertIsDisplayed().performClick()
        compose.onAllNodes(hasText(text(R.string.action_record_place))).fetchSemanticsNodes().let {
            assertTrue(it.isEmpty())
        }
    }

    @Test
    fun `T37 V30 창 2개 영역 홈 순서 — 맨 위 영역 · 시험 구역 · debug 카드 · 맨 위 영역은 스크롤 없이 보임`() {
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
                    HomeScreen(onOpenSettings = {}, showSdkCard = true, featureSections = tallSection)
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
    fun `T34 debug 카드를 끄면 홈에 없음`() {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    HomeScreen(onOpenSettings = {}, showSdkCard = false)
                }
            }
        }

        compose.onAllNodes(hasText(text(R.string.sdk_card_title))).fetchSemanticsNodes().let {
            assertTrue(it.isEmpty())
        }
    }

    private companion object {
        const val SECTION_TAG = "test_section"
    }
}
