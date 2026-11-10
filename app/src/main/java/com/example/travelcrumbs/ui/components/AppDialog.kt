package com.example.travelcrumbs.ui.components

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.travelcrumbs.ui.theme.Dimens
import com.example.travelcrumbs.ui.theme.TravelCrumbsColors

/** 팝업 틀의 Surface 태그 — 테스트가 「팝업이 떴다」를 볼 때. */
const val APP_DIALOG_TAG = "app_dialog"

/** 팝업 오른쪽(주요) 버튼 모양 — 디자인 시스템 12절. Danger 는 삭제 · 여행 종료 확인에만. */
enum class DialogConfirmKind { Primary, Danger }

/**
 * 앱의 모든 팝업 틀(디자인 시스템 18절 모달 · #51 사람 요청) — 화면 가운데 · 폭 제한 · 뒤 화면 어둡게(Background/Dim 검은색 60%).
 * 위에서 아래로 제목(headlineMedium) · [content](본문 · 입력 칸 · 칩 …) · 버튼 줄(왼쪽 [dismissLabel] 보조 · 오른쪽 [confirmLabel] 주요 또는 위험 · 같은 폭).
 * 바깥 누름 · 시스템 뒤로 = [onDismiss]. 창 열림 애니메이션 끔(주행 중 애니메이션 없음). 내용이 길면 세로 스크롤 · 키보드가 뜨면 창이 줄어 입력 칸이 가려지지 않음.
 * 폭은 플랫폼 기본(AAOS 테마마다 다름)에 맡기지 않고 창 폭 × [Dimens.DialogWidthFraction] 을 [Dimens.DialogMinWidth] ~ [Dimens.DialogMaxWidth] 로 자름.
 */
@Composable
fun AppDialog(
    title: String,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmKind: DialogConfirmKind = DialogConfirmKind.Primary,
    confirmEnabled: Boolean = true,
    buttonHeight: Dp = Dimens.HomeButtonHeight,
    titleMaxLines: Int = 2,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.apply {
                setWindowAnimations(0)
                setGravity(Gravity.CENTER)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(TravelCrumbsColors.BackgroundDimAlpha)
                @Suppress("DEPRECATION") // 창 안 내용(Compose)이 줄어든 높이에 맞춰 스크롤 — 입력 칸이 키보드에 가려지지 않게.
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(Dimens.ScreenPadding),
            contentAlignment = Alignment.Center,
        ) {
            val dialogWidth = (maxWidth * Dimens.DialogWidthFraction)
                .coerceIn(Dimens.DialogMinWidth, Dimens.DialogMaxWidth)
                .coerceAtMost(maxWidth)
            Surface(
                modifier = Modifier
                    .width(dialogWidth)
                    .testTag(APP_DIALOG_TAG),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    content()
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        val buttonModifier = Modifier
                            .weight(1f)
                            .widthIn(min = Dimens.MinTouchTarget)
                            .height(buttonHeight)
                        SecondaryButton(onClick = onDismiss, modifier = buttonModifier) {
                            DialogButtonText(dismissLabel)
                        }
                        when (confirmKind) {
                            DialogConfirmKind.Primary ->
                                PrimaryButton(onClick = onConfirm, enabled = confirmEnabled, modifier = buttonModifier) {
                                    DialogButtonText(confirmLabel)
                                }
                            DialogConfirmKind.Danger ->
                                DangerButton(onClick = onConfirm, enabled = confirmEnabled, modifier = buttonModifier) {
                                    DialogButtonText(confirmLabel)
                                }
                        }
                    }
                }
            }
        }
    }
}

/** 팝업 본문 한 덩이 — 본문 글자(titleLarge) · Text/Secondary 색. */
@Composable
fun AppDialogBody(text: String, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DialogButtonText(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
