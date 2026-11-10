package com.example.travelcrumbs.ui.components

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 팝업 공용 틀(#51 사람 요청 「팝업은 가운데 적당한 크기 · 뒤 화면 어둡게」).
 * DLG 번호 = 이 요청의 시험 번호(스펙 표 밖 · 보고서 「스펙 밖」).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1682dp-h1210dp-land")
class AppDialogTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dlg1_title_body_two_buttons_shown_and_clickable_width_limited() {
        var dismissed = 0
        var confirmed = 0
        compose.setContent {
            TravelCrumbsTheme {
                AppDialog(
                    title = "제목",
                    dismissLabel = "취소",
                    confirmLabel = "확인",
                    onDismiss = { dismissed++ },
                    onConfirm = { confirmed++ },
                ) {
                    Text(text = "본문")
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("제목").assertIsDisplayed()
        compose.onNodeWithText("본문").assertIsDisplayed()
        compose.onNode(hasText("취소") and hasClickAction()).assertIsDisplayed().performClick()
        compose.onNode(hasText("확인") and hasClickAction()).assertIsDisplayed().performClick()
        assertEquals(1, dismissed)
        assertEquals(1, confirmed)

        // 폭: 플랫폼 기본(화면 전체)이 아니라 창 폭 비율 · 최대 폭 안.
        val width = compose.onNodeWithTag(APP_DIALOG_TAG).getBoundsInRoot().width
        assertTrue("팝업 폭 $width", width >= Dimens.DialogMinWidth && width <= Dimens.DialogMaxWidth)
    }

    @Test
    fun dlg2_confirm_disabled_when_not_enabled() {
        compose.setContent {
            TravelCrumbsTheme {
                AppDialog(
                    title = "제목",
                    dismissLabel = "취소",
                    confirmLabel = "저장",
                    onDismiss = {},
                    onConfirm = {},
                    confirmKind = DialogConfirmKind.Danger,
                    confirmEnabled = false,
                )
            }
        }
        compose.waitForIdle()

        compose.onNode(hasText("저장") and hasClickAction()).assertIsNotEnabled()
    }
}
