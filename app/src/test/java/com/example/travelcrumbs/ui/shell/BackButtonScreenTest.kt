package com.example.travelcrumbs.ui.shell

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.testing.completedTrip
import com.example.travelcrumbs.ui.components.BACK_BUTTON_TAG
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.shell.components.railItemTag
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 하위 화면 머리글 「뒤로」(#51 사람 요청) — 버튼이 시스템 뒤로와 같은 곳으로 · 레일 첫 화면 · 주행 화면 · 주행 제한 안내엔 버튼 없음.
 * BK 번호 = 이 요청의 시험 번호(스펙 표 밖 · 보고서 「스펙 밖」).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class BackButtonScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = compose.activity.getString(id)

    private fun button(id: Int) = compose.onNode(hasText(text(id)) and hasClickAction())

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

    private fun rail(destination: AppDestination) {
        compose.onNodeWithTag(railItemTag(destination)).performClick()
        compose.waitForIdle()
    }

    private fun tapBack() {
        compose.onNodeWithTag(BACK_BUTTON_TAG).assertIsDisplayed().performClick()
        compose.waitForIdle()
    }

    private fun pressSystemBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun assertNoBackButton() {
        compose.onAllNodesWithTag(BACK_BUTTON_TAG).assertCountEquals(0)
    }

    @Test
    fun `BK1 레일 첫 화면 C-02 · A-07 · B-01 · C-03 — 뒤로 버튼 없음`() {
        show(ShellTestGraph(withTrip = false))

        button(R.string.action_start_trip).assertIsDisplayed()
        assertNoBackButton()
        rail(AppDestination.TRIPS)
        compose.onNodeWithText(text(R.string.trips_sub)).assertIsDisplayed()
        assertNoBackButton()
        rail(AppDestination.RECOMMENDATIONS)
        compose.onNodeWithText(text(R.string.reco_region_title)).assertIsDisplayed()
        assertNoBackButton()
        rail(AppDestination.SETTINGS)
        compose.onNodeWithText(text(R.string.settings_nickname)).assertIsDisplayed()
        assertNoBackButton()
    }

    @Test
    fun `BK2 주행 화면 · 주행 제한 안내 — 뒤로 버튼 없음`() {
        show(ShellTestGraph(initialDriving = DrivingState.DRIVING))

        compose.onNodeWithText(text(R.string.action_record_place)).assertIsDisplayed()
        assertNoBackButton()
        AppDestination.railItems.forEach { destination ->
            rail(destination)
            assertNoBackButton()
        }
    }

    @Test
    fun `BK3 A-01 여행 시작 — 뒤로 버튼 · 시스템 뒤로 모두 C-02 로(입력 버림 · 확인 없음)`() {
        show(ShellTestGraph(withTrip = false))

        button(R.string.action_start_trip).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.trip_start_title)).assertIsDisplayed()
        tapBack()
        button(R.string.action_start_trip).assertIsDisplayed()
        assertNoBackButton()

        button(R.string.action_start_trip).performClick()
        compose.waitForIdle()
        pressSystemBack()
        button(R.string.action_start_trip).assertIsDisplayed()
    }

    @Test
    fun `BK4 A-06 → A-05 → C-02 — 뒤로 버튼이 한 단계씩(시스템 뒤로와 같음)`() {
        val graph = ShellTestGraph(withTrip = false)
        graph.trips.put(completedTrip(places = 2))
        show(graph)

        compose.onNodeWithText(compose.activity.getString(R.string.review_title, "속초 가족여행")).assertIsDisplayed()
        button(R.string.review_save).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.publish_title)).assertIsDisplayed()

        tapBack()
        compose.onNodeWithText(compose.activity.getString(R.string.review_title, "속초 가족여행")).assertIsDisplayed()
        tapBack()
        button(R.string.action_start_trip).assertIsDisplayed()
        assertNoBackButton()
    }

    @Test
    fun `BK5 A-08 상세 — 뒤로 버튼 → A-07 목록 · 내 여행 칸 그대로`() {
        val graph = ShellTestGraph(withTrip = false)
        graph.trips.put(completedTrip())
        runBlocking { graph.trips.markReviewed(7) }
        show(graph)

        rail(AppDestination.TRIPS)
        compose.onNodeWithText("속초 가족여행").performClick()
        compose.waitForIdle()
        button(R.string.trip_rename_action).assertIsDisplayed()

        tapBack()
        compose.onNodeWithText(text(R.string.trips_sub)).assertIsDisplayed()
        compose.onNode(hasText(text(R.string.nav_trips)) and isSelected()).assertIsDisplayed()
        assertNoBackButton()
    }

    @Test
    fun `BK6 추천 B-04 → B-02 → B-01 · B-03 → B-02 — 뒤로 버튼이 한 단계씩 · B-01 엔 버튼 없음`() {
        show(ShellTestGraph(withTrip = false))
        rail(AppDestination.RECOMMENDATIONS)
        compose.onNode(hasText("강원") and hasClickAction()).performClick()
        compose.onNode(hasText("속초시") and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("속초 가족여행").performClick()
        compose.waitForIdle()
        button(R.string.reco_guide_from_first).assertIsDisplayed()

        tapBack()
        button(R.string.reco_open_tags).assertIsDisplayed().performClick()
        compose.waitForIdle()
        tapBack()
        button(R.string.reco_open_tags).assertIsDisplayed()
        tapBack()
        compose.onNodeWithText(text(R.string.reco_region_title)).assertIsDisplayed()
        compose.onNode(hasText(text(R.string.nav_recommendations)) and isSelected()).assertIsDisplayed()
        assertNoBackButton()
    }
}
