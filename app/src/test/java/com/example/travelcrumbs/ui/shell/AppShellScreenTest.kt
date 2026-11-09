package com.example.travelcrumbs.ui.shell

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.testing.FAKE_INPUT_TAG
import com.example.travelcrumbs.testing.FakeInputContent
import com.example.travelcrumbs.testing.ShellTestGraph
import com.example.travelcrumbs.ui.navigation.AppDestination
import com.example.travelcrumbs.ui.shell.components.railItemTag
import com.example.travelcrumbs.ui.shell.components.railIconTag
import com.example.travelcrumbs.ui.shell.components.railLabelTag
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.RailColors
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 앱 틀 Compose UI 테스트(Robolectric · 창 2개 영역 = 에뮬레이터 실측 앱 창 1682 × 1210 dp · #6 D18). */
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

    private fun rail(destination: AppDestination): SemanticsNodeInteraction =
        compose.onNodeWithTag(railItemTag(destination))

    private fun assertHighlighted(destination: AppDestination) {
        compose.onNode(hasText(text(destination.labelRes)) and isSelected()).assertIsDisplayed()
    }

    private fun button(id: Int) = compose.onNode(hasText(text(id)) and hasClickAction())

    /** 앱이 뒤로를 잡지 않으면 시스템 기본 동작 — 테스트 액티비티는 끝남(실차 런처 액티비티는 뒤로 감). */
    private fun systemHandledBack(): Boolean = compose.activity.isFinishing

    @Test
    fun `V1 V67 V68 레일 위에서 아래로 기록 내 여행 추천 설정 4칸 · 기록 강조 · 기록 칸 · 설정 버튼 없음`() {
        show()

        val tops = AppDestination.railItems.map { rail(it).assertIsDisplayed().getBoundsInRoot().top }
        assertEquals(tops.sorted(), tops)
        assertEquals(4, tops.size)
        assertEquals(
            listOf(R.string.nav_records, R.string.nav_trips, R.string.nav_recommendations, R.string.nav_settings),
            AppDestination.railItems.map { it.labelRes },
        )
        assertHighlighted(AppDestination.RECORDS)
        // 기록 칸 · 진행 중(첫 실행) 모양
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        assertOnlyRailSettings()
    }

    @Test
    fun `V2 V3 V4 내 여행 추천 설정 — 제목 · 준비 중 · 그 칸 강조`() {
        show()

        listOf(AppDestination.TRIPS, AppDestination.RECOMMENDATIONS, AppDestination.SETTINGS).forEach { destination ->
            rail(destination).performClick()
            assertHighlighted(destination)
            compose.onAllNodes(hasText(text(destination.labelRes))).assertCountEquals(2)
            compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        }
    }

    @Test
    fun `V6 설정에서 뒤로 — 기록 · 기록에서 뒤로는 앱이 처리 안 함`() {
        show()
        rail(AppDestination.SETTINGS).performClick()

        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        pressBack()
        assertHighlighted(AppDestination.RECORDS)
        compose.onNodeWithText(text(R.string.action_record_place)).assertIsDisplayed()
        assertFalse(systemHandledBack())
    }

    @Test
    fun `V7 T4 내 여행 → 추천에서 뒤로 — 기록 · 기록에서 뒤로는 앱이 처리 안 함`() {
        show()
        rail(AppDestination.TRIPS).performClick()
        rail(AppDestination.RECOMMENDATIONS).performClick()

        pressBack()
        assertHighlighted(AppDestination.RECORDS)
        assertFalse(systemHandledBack())

        pressBack()
        assertTrue(systemHandledBack())
    }

    @Test
    fun `V8 V67 어느 칸에도 설정 버튼 없음 — 레일 칸만`() {
        show()
        assertOnlyRailSettings()

        AppDestination.railItems.forEach { destination ->
            rail(destination).performClick()
            assertOnlyRailSettings()
        }
        // 진행 중 아님 모양에도 없음
        drive(DrivingState.DRIVING)
        button(R.string.action_end_trip).performClick()
        button(R.string.end_trip_confirm_end).performClick()
        drive(DrivingState.PARKED)
        rail(AppDestination.RECORDS).performClick()
        button(R.string.action_start_trip).assertIsDisplayed()
        assertOnlyRailSettings()
    }

    /** 「설정」 글자로 눌리는 것은 레일 칸 하나뿐(D35). */
    private fun assertOnlyRailSettings() {
        compose.onAllNodes(hasText(text(R.string.nav_settings)) and hasClickAction()).assertCountEquals(1)
        rail(AppDestination.SETTINGS).assertIsDisplayed()
    }

    @Test
    fun `V9 설정에서 주행 — 레일 보임 · 강조 기록 · 주행 전용 화면 → 정차하면 설정 · 강조 설정`() {
        show()
        rail(AppDestination.SETTINGS).performClick()

        drive(DrivingState.DRIVING)
        AppDestination.railItems.forEach { rail(it).assertIsDisplayed() }
        assertHighlighted(AppDestination.RECORDS)
        button(R.string.action_end_trip).assertIsDisplayed()

        drive(DrivingState.PARKED)
        assertHighlighted(AppDestination.SETTINGS)
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
    }

    @Test
    fun `V11 V47 주행 첫 실행 — 레일 4칸 · 새 여행 · 기록 중 · 0분 · 장소 0 · 지도 자리 · 버튼 4개뿐`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        assertDrivingScreen()
    }

    @Test
    fun `V12 판정 모름 — 주행과 같음`() {
        graph.driving.emit(DrivingState.UNKNOWN)
        show()

        assertDrivingScreen()
    }

    private fun assertDrivingScreen() {
        AppDestination.railItems.forEach { rail(it).assertIsDisplayed() }
        assertHighlighted(AppDestination.RECORDS)
        compose.onNodeWithText(text(R.string.trip_name_default)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.map_placeholder)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsDisplayed().assertIsEnabled()
        button(R.string.action_voice).assertIsDisplayed().assertIsEnabled()
        button(R.string.action_pause_recording).assertIsDisplayed().assertIsEnabled()
        button(R.string.action_end_trip).assertIsDisplayed().assertIsEnabled()
        // 눌리는 것 = 레일 4칸 + 버튼 4개뿐
        compose.onAllNodes(hasClickAction()).assertCountEquals(AppDestination.railItems.size + 4)
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        compose.onAllNodes(hasText(text(R.string.unfinished_input_banner_text))).assertCountEquals(0)
        compose.onAllNodes(hasText(text(R.string.action_start_trip))).assertCountEquals(0)
    }

    @Test
    fun `V13 V14 여행 종료 → 팝업 → 계속 기록 — 팝업 닫힘 · 기록 중`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        button(R.string.action_end_trip).performClick()
        compose.onNodeWithText(text(R.string.end_trip_confirm_body)).assertIsDisplayed()
        button(R.string.end_trip_confirm_end).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()

        button(R.string.end_trip_confirm_continue).performClick()
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
    }

    @Test
    fun `V15 V18 V25 종료 — 완료 · 버튼 비활성 · 음성 활성 · 주행 화면 그대로 → 정차 기록 칸은 진행 중 아님 모양`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        button(R.string.action_end_trip).performClick()
        button(R.string.end_trip_confirm_end).performClick()

        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.trip_status_completed)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsNotEnabled()
        button(R.string.action_pause_recording).assertIsNotEnabled()
        button(R.string.action_end_trip).assertIsNotEnabled()
        button(R.string.action_voice).assertIsEnabled().performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()

        drive(DrivingState.PARKED)
        assertNoTripTop()
    }

    /** 기록 칸 진행 중 아님 모양 — 「여행 시작」 · 「최근 여행이 없어요」 · 「추천 보러 가기」만 · 진행 중 요소 없음(D34). */
    private fun assertNoTripTop() {
        button(R.string.action_start_trip).assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText(text(R.string.recent_trip_empty)).assertIsDisplayed()
        button(R.string.action_open_recommendations).assertIsDisplayed().assertIsEnabled()
        listOf(
            R.string.trip_status_recording,
            R.string.recording_status_paused,
            R.string.trip_status_completed,
            R.string.action_record_place,
        ).forEach { compose.onAllNodes(hasText(text(it))).assertCountEquals(0) }
        compose.onAllNodes(hasText(compose.activity.getString(R.string.trip_place_count, 0))).assertCountEquals(0)
    }

    /** 기록 칸 진행 중 모양 — 「여행 시작」 · 최근 여행 자리 · 「추천 보러 가기」 없음(비활성으로 남지 않음 · V35). */
    private fun assertNoStartTripElements() {
        compose.onAllNodes(hasText(text(R.string.action_start_trip))).assertCountEquals(0)
        compose.onAllNodes(hasText(text(R.string.recent_trip_empty))).assertCountEquals(0)
        compose.onAllNodes(hasText(text(R.string.action_open_recommendations))).assertCountEquals(0)
    }

    @Test
    fun `V68 V69 완료 뒤 기록 칸 — 추천 보러 가기 → 추천 칸 · 강조 추천 → 뒤로 기록`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()
        button(R.string.action_end_trip).performClick()
        button(R.string.end_trip_confirm_end).performClick()
        drive(DrivingState.PARKED)

        button(R.string.action_open_recommendations).performClick()
        assertHighlighted(AppDestination.RECOMMENDATIONS)
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()

        pressBack()
        assertHighlighted(AppDestination.RECORDS)
        assertNoTripTop()
    }

    @Test
    fun `V70 V71 기록 칸 첫 실행 — 기록 중 · 0분 · 장소 0 · 이 장소 기록 → 준비 중 안내 · 화면 이동 없음`() {
        show()

        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        assertNoStartTripElements()

        button(R.string.action_record_place).performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        assertHighlighted(AppDestination.RECORDS)
        compose.mainClock.advanceTimeBy(AppConfig.NOT_READY_NOTICE_MILLIS + FRAME_MILLIS)
        compose.onAllNodes(hasText(text(R.string.not_ready))).assertCountEquals(0)
    }

    @Test
    fun `V17 팝업 열린 채 정차 → 다시 주행 — 기록 칸(팝업 없음 · 기록 중) → 팝업 저절로 안 열림`() {
        show()
        drive(DrivingState.DRIVING)
        button(R.string.action_end_trip).performClick()

        drive(DrivingState.PARKED)
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        assertNoStartTripElements()

        drive(DrivingState.DRIVING)
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
    }

    @Test
    fun `V24 준비 중 안내가 보였다가 사라짐 · 화면 이동 없음`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        button(R.string.action_record_place).performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        button(R.string.action_end_trip).assertIsDisplayed()

        compose.mainClock.advanceTimeBy(AppConfig.NOT_READY_NOTICE_MILLIS + FRAME_MILLIS)
        compose.onAllNodes(hasText(text(R.string.not_ready))).assertCountEquals(0)

        button(R.string.action_voice).performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
    }

    @Test
    fun `V27 T21 T22 T23 입력 중 주행 → 작성 중 내용 보존 · 정차 → 띠 · 이어서 쓰기 → 복원`() {
        show { destination ->
            if (destination == AppDestination.TRIPS) {
                FakeInputContent(graph.inputs, destination.id)
            } else {
                Text(destination.id)
            }
        }
        rail(AppDestination.TRIPS).performClick()
        compose.onNodeWithTag(FAKE_INPUT_TAG).performTextInput("바다 앞 카페")

        drive(DrivingState.DRIVING)
        compose.onAllNodes(hasText(text(R.string.unfinished_input_banner_text))).assertCountEquals(0)

        drive(DrivingState.PARKED)
        compose.onNodeWithText(text(R.string.unfinished_input_banner_text)).assertIsDisplayed()
        compose.onNodeWithTag(FAKE_INPUT_TAG).assertTextEquals("")

        compose.onNodeWithText(text(R.string.unfinished_input_banner_action)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(FAKE_INPUT_TAG).assertTextEquals("바다 앞 카페")
        compose.onAllNodes(hasText(text(R.string.unfinished_input_banner_text))).assertCountEquals(0)
    }

    @Test
    fun `T23 다른 목적지에 있다가 이어서 쓰기 — 저장 당시 목적지로 이동`() {
        show { destination ->
            if (destination == AppDestination.TRIPS) {
                FakeInputContent(graph.inputs, destination.id)
            } else {
                Text(destination.id)
            }
        }
        rail(AppDestination.TRIPS).performClick()
        compose.onNodeWithTag(FAKE_INPUT_TAG).performTextInput("메모")
        drive(DrivingState.DRIVING)
        drive(DrivingState.PARKED)
        rail(AppDestination.RECORDS).performClick()

        compose.onNodeWithText(text(R.string.unfinished_input_banner_action)).performClick()
        compose.waitForIdle()

        assertHighlighted(AppDestination.TRIPS)
        compose.onNodeWithTag(FAKE_INPUT_TAG).assertTextEquals("메모")
    }

    @Test
    fun `T24 입력 없이 정차 주행 정차 — 띠 없음`() {
        show()

        drive(DrivingState.DRIVING)
        drive(DrivingState.PARKED)

        compose.onAllNodes(hasText(text(R.string.unfinished_input_banner_text))).assertCountEquals(0)
    }

    @Test
    fun `V33 V35 V56 기록 칸 기록 중 · 일시정지 각각 — 일시정지 · 재개 · 음성 · 여행 종료 · 설정 버튼 없음 · 여행 시작 없음`() {
        show()
        assertHomeHasNoDrivingButtons()
        assertNoStartTripElements()

        drive(DrivingState.DRIVING)
        button(R.string.action_pause_recording).performClick()
        drive(DrivingState.PARKED)

        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        assertHomeHasNoDrivingButtons()
        button(R.string.action_record_place).assertIsEnabled()
        assertNoStartTripElements()
    }

    private fun assertHomeHasNoDrivingButtons() {
        button(R.string.action_pause_recording).assertDoesNotExist()
        button(R.string.action_resume_recording).assertDoesNotExist()
        button(R.string.action_voice).assertDoesNotExist()
        button(R.string.action_end_trip).assertDoesNotExist()
        compose.onAllNodes(hasText(text(R.string.nav_settings)) and hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun `V47 주행 버튼 순서 — 이 장소 기록 · 음성 · 기록 일시정지 · 여행 종료(위 → 아래 · 왼쪽 → 오른쪽)`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        val order = listOf(
            R.string.action_record_place,
            R.string.action_voice,
            R.string.action_pause_recording,
            R.string.action_end_trip,
        ).map { id -> id to button(id).getBoundsInRoot() }
        val sorted = order.sortedWith(compareBy({ it.second.top.value }, { it.second.left.value })).map { it.first }
        assertEquals(order.map { it.first }, sorted)
    }

    @Test
    fun `V48 V49 기록 일시정지 → 일시정지 · 기록 재개 · 팝업 없음 → 기록 재개 → 기록 중`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        button(R.string.action_pause_recording).performClick()

        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        button(R.string.action_resume_recording).assertIsDisplayed().assertIsEnabled()
        button(R.string.action_pause_recording).assertDoesNotExist()
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        assertHighlighted(AppDestination.RECORDS)

        button(R.string.action_resume_recording).performClick()

        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        button(R.string.action_pause_recording).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `V51 V53 V54 일시정지 중 이 장소 기록 가능 · 여행 종료 → 계속 기록이면 일시정지 그대로 → 종료면 완료`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()
        button(R.string.action_pause_recording).performClick()

        button(R.string.action_record_place).assertIsEnabled().performClick()
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()

        button(R.string.action_end_trip).assertIsEnabled().performClick()
        compose.onNodeWithText(text(R.string.end_trip_confirm_body)).assertIsDisplayed()
        button(R.string.end_trip_confirm_continue).performClick()
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()
        button(R.string.action_resume_recording).assertIsEnabled()

        button(R.string.action_end_trip).performClick()
        button(R.string.end_trip_confirm_end).performClick()
        compose.onNodeWithText(text(R.string.trip_status_completed)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsNotEnabled()
        button(R.string.action_pause_recording).assertIsNotEnabled()
        button(R.string.action_end_trip).assertIsNotEnabled()
        button(R.string.action_voice).assertIsEnabled()
    }

    @Test
    fun `V55 V65 일시정지 · 팝업 열린 채 정차 → 팝업 없음 · 일시정지 → 다시 주행에도 일시정지`() {
        show()
        drive(DrivingState.DRIVING)
        button(R.string.action_pause_recording).performClick()
        button(R.string.action_end_trip).performClick()

        drive(DrivingState.PARKED)
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()

        drive(DrivingState.DRIVING)
        compose.onAllNodes(hasText(text(R.string.end_trip_confirm_body))).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.recording_status_paused)).assertIsDisplayed()
        button(R.string.action_resume_recording).assertIsDisplayed()
    }

    @Test
    fun `V34 V36 완료 뒤 기록 칸 여행 시작 — 진행 중 모양 · 기록 중 · 0분 · 장소 0 → 주행 화면도 같은 여행`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()
        button(R.string.action_end_trip).performClick()
        button(R.string.end_trip_confirm_end).performClick()
        drive(DrivingState.PARKED)

        button(R.string.action_start_trip).performClick()

        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.elapsed_minutes, 0)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsEnabled()
        assertNoStartTripElements()

        drive(DrivingState.DRIVING)
        compose.onNodeWithText(text(R.string.trip_name_default)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.trip_place_count, 0)).assertIsDisplayed()
        button(R.string.action_record_place).assertIsEnabled()
        button(R.string.action_pause_recording).assertIsEnabled()
        button(R.string.action_end_trip).assertIsEnabled()
    }

    @Test
    fun `V39 V42 주행 중 레일 내 여행 — 제목 · 정차 후 볼 수 있어요 · 강조 내 여행 → 기록으로 돌아옴`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        rail(AppDestination.TRIPS).performClick()
        assertHighlighted(AppDestination.TRIPS)
        compose.onAllNodes(hasText(text(R.string.nav_trips))).assertCountEquals(2)
        compose.onNodeWithText(text(R.string.driving_restricted)).assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(AppDestination.railItems.size)
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)

        rail(AppDestination.RECORDS).performClick()
        assertHighlighted(AppDestination.RECORDS)
        compose.onNodeWithText(text(R.string.trip_status_recording)).assertIsDisplayed()
        button(R.string.action_end_trip).assertIsDisplayed()
    }

    @Test
    fun `V40 주행 중 레일 설정 — 제목 · 정차 후 볼 수 있어요 · 설정 내용 없음`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        rail(AppDestination.SETTINGS).performClick()

        assertHighlighted(AppDestination.SETTINGS)
        compose.onAllNodes(hasText(text(R.string.nav_settings))).assertCountEquals(2)
        compose.onNodeWithText(text(R.string.driving_restricted)).assertIsDisplayed()
        compose.onAllNodes(hasText(text(R.string.not_ready))).assertCountEquals(0)
        button(R.string.action_record_place).assertDoesNotExist()
        compose.onAllNodes(hasText(text(R.string.sdk_card_title))).assertCountEquals(0)
        compose.onAllNodes(hasClickAction()).assertCountEquals(AppDestination.railItems.size)
    }

    @Test
    fun `V41 주행 중 레일 추천 — 제목 + 정차 후 볼 수 있어요`() {
        graph.driving.emit(DrivingState.DRIVING)
        show()

        listOf(AppDestination.RECOMMENDATIONS).forEach { destination ->
            rail(destination).performClick()
            compose.onAllNodes(hasText(text(destination.labelRes))).assertCountEquals(2)
            compose.onNodeWithText(text(R.string.driving_restricted)).assertIsDisplayed()
        }
    }

    @Test
    fun `V43 정차 기록 → 주행 → 레일 내 여행 → 정차 — 내 여행 정차 화면 · 강조 내 여행`() {
        show()
        drive(DrivingState.DRIVING)
        rail(AppDestination.TRIPS).performClick()

        drive(DrivingState.PARKED)

        assertHighlighted(AppDestination.TRIPS)
        compose.onNodeWithText(text(R.string.not_ready)).assertIsDisplayed()
        compose.onAllNodes(hasText(text(R.string.driving_restricted))).assertCountEquals(0)
    }

    @Test
    fun `V44 T28 주행 제한 안내에서 뒤로 — 앱이 잡지 않아 정차 내용이 드러나지 않음`() {
        show()
        rail(AppDestination.SETTINGS).performClick()
        drive(DrivingState.DRIVING)
        rail(AppDestination.RECOMMENDATIONS).performClick()

        pressBack()

        // 정차 칸의 뒤로(설정 → 기록)가 아니라 시스템 기본 동작
        assertTrue(systemHandledBack())
    }

    @Test
    fun `V46 레일 칸 터치 영역 48 × 48dp 이상 — 주행 · 정차 같음`() {
        show()
        assertRailTouchTargets()

        drive(DrivingState.DRIVING)
        assertRailTouchTargets()
    }

    @Test
    fun `V46 레일 글자 5 mm 이상(160 dpi 기준) · 한 줄 · 칸 안에 들어감 · 4칸이 화면 높이 안 — 주행 · 정차 같음`() {
        show()
        assertRailLabels()

        drive(DrivingState.DRIVING)
        assertRailLabels()
    }

    private fun assertRailLabels() {
        val railBottoms = AppDestination.railItems.map { destination ->
            val item = rail(destination).getBoundsInRoot()
            val label = compose.onNodeWithTag(railLabelTag(destination), useUnmergedTree = true)
                .assertIsDisplayed()
                .getBoundsInRoot()
            assertTrue("${destination.id} 글자 높이 ${label.height}", label.height >= FIVE_MM_AT_160_DPI)
            assertTrue("${destination.id} 한 줄", label.height < FIVE_MM_AT_160_DPI * 2)
            assertTrue("${destination.id} 글자가 칸 폭 안", label.left >= item.left && label.right <= item.right)
            item.bottom
        }
        val root = compose.onRoot().getBoundsInRoot()
        assertTrue("레일 4칸이 화면 높이 안 · ${railBottoms.max()}", railBottoms.max() <= root.bottom)
    }

    @Test
    fun `V72 V73 레일 시각 — 4칸 모두 아이콘 32dp 이상 + 글자 · 선택 칸 배경 아이콘 글자 색이 기본 칸과 다름 — 정차 · 주행`() {
        show()
        assertRailVisuals(AppDestination.RECORDS)

        rail(AppDestination.TRIPS).performClick()
        assertRailVisuals(AppDestination.TRIPS)

        drive(DrivingState.DRIVING)
        assertRailVisuals(AppDestination.RECORDS)
    }

    private fun assertRailVisuals(selected: AppDestination) {
        AppDestination.railItems.forEach { destination ->
            val icon = compose.onNodeWithTag(railIconTag(destination), useUnmergedTree = true)
                .assertIsDisplayed()
                .getBoundsInRoot()
            assertTrue("${destination.id} 아이콘 ${icon.width}", icon.width >= Dimens.RailIconSize && icon.height >= Dimens.RailIconSize)
            compose.onNodeWithTag(railLabelTag(destination), useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(text(destination.labelRes))
            assertEquals(destination == selected, rail(destination).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))
        }
        // 색 규칙(10절 토큰)은 RailColors 한 곳 — 선택 · 기본의 배경 · 아이콘 · 글자 세 가지가 모두 다름
        assertTrue(RailColors.BackgroundSelected != RailColors.UnselectedBackground)
        assertTrue(RailColors.BrandSecondary != RailColors.Unselected)
        assertEquals(androidx.compose.ui.graphics.Color.Transparent, RailColors.UnselectedBackground)
        assertEquals(androidx.compose.ui.graphics.Color.White, RailColors.Unselected)
    }

    private fun assertRailTouchTargets() {
        AppDestination.railItems.forEach { destination ->
            val bounds = rail(destination).getBoundsInRoot()
            assertTrue("${destination.id} 폭 ${bounds.width}", bounds.width >= Dimens.MinTouchTarget)
            assertTrue("${destination.id} 높이 ${bounds.height}", bounds.height >= Dimens.MinTouchTarget)
        }
    }

    private companion object {
        const val FRAME_MILLIS = 32L

        /** 5 mm · 160 dpi(에뮬레이터 Pleos_Connect_v2.1.0 · 1dp = 1px) = 5 / 25.4 × 160 ≈ 31.5dp. */
        val FIVE_MM_AT_160_DPI = (5f / 25.4f * 160f).dp
    }
}
