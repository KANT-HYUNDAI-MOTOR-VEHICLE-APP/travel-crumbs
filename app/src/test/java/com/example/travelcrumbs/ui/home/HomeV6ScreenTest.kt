package com.example.travelcrumbs.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.Visibility
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.testing.completedTrip
import com.example.travelcrumbs.ui.shell.AppShellScreen
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** C-02 기록 첫 화면(와이어프레임 v6 · #51 U2) — 넓은 칸 · 좁은 칸. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class HomeV6ScreenTest {

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

    @Test
    fun `U2-1 여행 없음 — 인사 · 여행 시작 · 추천 보러 가기 한 줄 · 최근 여행 카드 공개 중`() {
        val graph = ShellTestGraph(withTrip = false)
        graph.trips.put(completedTrip(visibility = Visibility.ANONYMOUS_PUBLIC))
        show(graph)

        compose.onNodeWithText(compose.activity.getString(R.string.home_greeting, "민지네")).assertIsDisplayed()
        val start = button(R.string.action_start_trip).assertIsDisplayed().getBoundsInRoot()
        val reco = button(R.string.action_open_recommendations).assertIsDisplayed().getBoundsInRoot()
        assertTrue("넓은 칸은 한 줄", start.top == reco.top && reco.left > start.right)
        compose.onNodeWithText("속초 가족여행").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.trip_public_badge)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w817dp-h1210dp-port")
    fun `U2-6 좁은 칸 여행 없음 — 두 카드가 위아래 · 최근 여행 없음 안내`() {
        show(ShellTestGraph(withTrip = false))

        val start = button(R.string.action_start_trip).assertIsDisplayed().getBoundsInRoot()
        val reco = button(R.string.action_open_recommendations).assertIsDisplayed().getBoundsInRoot()
        assertTrue("좁은 칸은 위아래", reco.top >= start.bottom)
        compose.onNodeWithText(text(R.string.recent_trip_empty)).assertIsDisplayed()
    }

    @Test
    fun `U2-2 진행 중 정차 — 여행 종료 → A-04 팝업 → 종료 → 여행 없음 모양`() {
        show(ShellTestGraph())

        button(R.string.action_end_trip).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.end_trip_confirm_body)).assertIsDisplayed()
        button(R.string.end_trip_confirm_end).performClick()
        compose.waitForIdle()

        button(R.string.action_start_trip).assertIsDisplayed()
    }
}
