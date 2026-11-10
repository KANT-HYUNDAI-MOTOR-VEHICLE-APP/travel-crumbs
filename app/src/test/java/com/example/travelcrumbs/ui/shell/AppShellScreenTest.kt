package com.example.travelcrumbs.ui.shell

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoCommandState
import com.example.travelcrumbs.testing.FAKE_INPUT_TAG
import com.example.travelcrumbs.testing.FakeInputContent
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 앱 틀 Compose UI 테스트(Robolectric · 창 2개 영역 = 에뮬레이터 실측 앱 창 1682 × 1210 dp · D18). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class AppShellScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val graph = ShellTestGraph()

    private fun text(id: Int) = compose.activity.getString(id)

    private fun show(destinationContent: (@Composable (AppDestination) -> Unit)? = null) {
        compose.setContent {
            TravelCrumbsTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides graph.owner) {
                    if (destinationContent == null) {
                        AppShellScreen()
                    } else {
                        AppShellScreen(destinationContent = destinationContent)
                    }
                }
            }
        }
    }

    private fun drive(state: DrivingState) {
        graph.driving.emit(state)
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** 앱이 뒤로를 잡지 않으면 시스템 기본 동작 — 테스트 액티비티는 끝남(실차 런처 액티비티는 뒤로 감). */
    private fun systemHandledBack(): Boolean = compose.activity.isFinishing

    @Test
    fun `T1 정차면 레일 홈 여행 설정 · 홈 선택`() {
        show()

        compose.onNode(hasText(text(R.string.nav_home)) and isSelected()).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_trips)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_settings)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_record_place)).assertIsDisplayed()
    }

    @Test
    fun `T2 여행 누르면 여행 목록 · 여행 강조`() {
        show()

        compose.onNodeWithText(text(R.string.nav_trips)).performClick()

        compose.onNode(hasText(text(R.string.nav_trips)) and isSelected()).assertIsDisplayed()
        // 여행 목록(A-07 간단판) — 테스트 그래프에는 여행이 없음
        compose.onNodeWithText(text(R.string.trips_empty)).assertIsDisplayed()
    }

    @Test
    fun `T3 T4 설정에서 뒤로는 홈 · 홈에서 뒤로는 앱이 처리 안 함`() {
        show()
        compose.onNodeWithText(text(R.string.nav_trips)).performClick()
        compose.onNodeWithText(text(R.string.nav_settings)).performClick()

        pressBack()
        compose.onNode(hasText(text(R.string.nav_home)) and isSelected()).assertIsDisplayed()

        assertFalse(systemHandledBack())

        // 홈에선 앱이 뒤로를 잡지 않음 → 시스템이 앱을 뒤로 보냄
        pressBack()
        assertTrue(systemHandledBack())
    }

    @Test
    fun `T5 T8 주행이면 레일 없이 주행 화면 · 정차로 오면 직전 목적지`() {
        show()
        compose.onNodeWithText(text(R.string.nav_settings)).performClick()

        drive(DrivingState.DRIVING)
        compose.onNodeWithText(text(R.string.action_pause_recording)).assertIsDisplayed()
        compose.onAllNodes(hasText(text(R.string.nav_home))).assertCountEquals(0)

        drive(DrivingState.PARKED)
        compose.onNode(hasText(text(R.string.nav_settings)) and isSelected()).assertIsDisplayed()
    }

    @Test
    fun `T9 판정 모름이면 주행 화면`() {
        graph.driving.emit(DrivingState.UNKNOWN)
        show()

        compose.onNodeWithText(text(R.string.action_voice_input)).assertIsDisplayed()
        compose.onAllNodes(hasText(text(R.string.nav_home))).assertCountEquals(0)
    }

    @Test
    fun `T14 T15 준비 중 안내가 보였다가 사라짐 · 화면 이동 없음`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        compose.onNodeWithText(text(R.string.action_record_place)).performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_pause_recording)).assertIsDisplayed()

        compose.mainClock.advanceTimeBy(AppConfig.NOT_READY_NOTICE_MILLIS + FRAME_MILLIS)
        compose.onAllNodes(hasText(text(R.string.not_ready))).assertCountEquals(0)

        compose.onNodeWithText(text(R.string.action_voice_input)).performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
    }

    @Test
    fun `T17 T18 T19 일시정지 토글 · 정차 홈도 같은 상태`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        compose.onNodeWithText(text(R.string.action_pause_recording)).performClick()
        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_resume_recording)).assertIsDisplayed()

        drive(DrivingState.PARKED)
        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()

        drive(DrivingState.DRIVING)
        compose.onNodeWithText(text(R.string.action_resume_recording)).performClick()
        compose.onNodeWithText(text(R.string.recording_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_pause_recording)).assertIsDisplayed()
    }

    @Test
    fun `T20 시작은 경과 0분`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
    }

    @Test
    fun `T21 T22 T23 입력 중 주행 → 임시 저장 · 정차 → 띠 · 이어서 쓰기 → 복원`() {
        show { destination ->
            if (destination == AppDestination.TRIPS) {
                FakeInputContent(graph.drafts, destination.id)
            } else {
                Text(destination.id)
            }
        }
        compose.onNodeWithText(text(R.string.nav_trips)).performClick()
        compose.onNodeWithTag(FAKE_INPUT_TAG).performTextInput("바다 앞 카페")

        drive(DrivingState.DRIVING)
        compose.onAllNodes(hasText(text(R.string.draft_banner_text))).assertCountEquals(0)

        drive(DrivingState.PARKED)
        compose.onNodeWithText(text(R.string.draft_banner_text)).assertIsDisplayed()
        compose.onNodeWithTag(FAKE_INPUT_TAG).assertTextEquals("")

        compose.onNodeWithText(text(R.string.draft_banner_action)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(FAKE_INPUT_TAG).assertTextEquals("바다 앞 카페")
        compose.onAllNodes(hasText(text(R.string.draft_banner_text))).assertCountEquals(0)
    }

    @Test
    fun `T23 다른 목적지에 있다가 이어서 쓰기 — 저장 당시 목적지로 이동`() {
        show { destination ->
            if (destination == AppDestination.TRIPS) {
                FakeInputContent(graph.drafts, destination.id)
            } else {
                Text(destination.id)
            }
        }
        compose.onNodeWithText(text(R.string.nav_trips)).performClick()
        compose.onNodeWithTag(FAKE_INPUT_TAG).performTextInput("메모")
        drive(DrivingState.DRIVING)
        drive(DrivingState.PARKED)
        compose.onNodeWithText(text(R.string.nav_home)).performClick()

        compose.onNodeWithText(text(R.string.draft_banner_action)).performClick()
        compose.waitForIdle()

        compose.onNode(hasText(text(R.string.nav_trips)) and isSelected()).assertIsDisplayed()
        compose.onNodeWithTag(FAKE_INPUT_TAG).assertTextEquals("메모")
    }

    @Test
    fun `T24 입력 없이 정차 주행 정차 — 띠 없음`() {
        show()

        drive(DrivingState.DRIVING)
        drive(DrivingState.PARKED)

        compose.onAllNodes(hasText(text(R.string.draft_banner_text))).assertCountEquals(0)
    }

    @Test
    fun `Gleo T24 정차의 여행 복원 진행 표시를 주행과 UNKNOWN에서 정적인 한 줄로 바꿈`() {
        graph.gleo.commandState.value = GleoCommandState(1L, GleoCommandPhase.LOADING_TRIP)
        show()
        val progress = SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)

        compose.onNodeWithText(text(R.string.gleo_loading_trip_parked)).assertIsDisplayed()
        compose.onNode(progress).assertIsDisplayed()
        for (state in listOf(DrivingState.DRIVING, DrivingState.UNKNOWN)) {
            drive(state)
            compose.onNodeWithText(text(R.string.gleo_loading_trip)).assertIsDisplayed()
            compose.onAllNodes(progress).assertCountEquals(0)
            compose.onAllNodes(hasText(text(R.string.gleo_loading_trip_parked))).assertCountEquals(0)
        }
    }

    @Test
    fun `T25 T36 주행 화면 — 버튼 3개뿐 · 입력 칸 스크롤 레일 길안내 없음`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        compose.onAllNodes(hasClickAction()).assertCountEquals(3)
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        compose.onAllNodes(hasText(text(R.string.nav_home))).assertCountEquals(0)
        compose.onAllNodes(hasText("안내", substring = true)).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.action_record_place)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_voice_input)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_pause_recording)).assertIsDisplayed()
    }

    @Test
    fun `T28 주행 화면에서 뒤로 — 앱이 잡지 않아 정차 화면이 드러나지 않음`() {
        show()
        compose.onNodeWithText(text(R.string.nav_settings)).performClick()
        drive(DrivingState.DRIVING)

        pressBack()

        // 레일의 뒤로(설정 → 홈)가 아니라 시스템 기본 동작
        assertTrue(systemHandledBack())
    }

    @Test
    fun `T35 홈에는 기록 일시정지 · 음성 입력 없음`() {
        show()

        compose.onAllNodes(hasText(text(R.string.action_pause_recording))).assertCountEquals(0)
        compose.onAllNodes(hasText(text(R.string.action_voice_input))).assertCountEquals(0)
    }

    private companion object {
        const val FRAME_MILLIS = 32L
    }
}
