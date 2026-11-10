package com.example.travelcrumbs.ui.components

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

/** 팝업 뒤 덮개 태그 — 화면 전체 · 눌러도 닫히지 않음. */
const val APP_DIALOG_SCRIM_TAG = "app_dialog_scrim"

/** 팝업 오른쪽(주요) 버튼 모양 — 디자인 시스템 12절. Danger 는 삭제 · 여행 종료 확인에만. */
enum class DialogConfirmKind { Primary, Danger }

/**
 * 앱의 모든 팝업 틀(디자인 시스템 18절 모달 · #51 사람 요청) — 화면 가운데 · 폭 제한 · 뒤 화면 어둡게(Background/Dim 검은색 60%).
 * 창 dim(FLAG_DIM_BEHIND)은 AAOS 에서 무시됨(사람 확인) — 창을 화면 전체로 만들고 덮개를 Compose 로 직접 그림.
 * 위에서 아래로 제목(headlineMedium) · [content](본문 · 입력 칸 · 칩 …) · 버튼 줄(왼쪽 [dismissLabel] 보조 · 오른쪽 [confirmLabel] 주요 또는 위험 · 같은 폭).
 * 시스템 뒤로 = [onDismiss] · 덮개(바깥) 누름은 아무 일 없음(사람 결정 「바깥 양옆 눌러도 안 닫히게」). 창 열림 애니메이션 끔(주행 중 애니메이션 없음). 내용이 길면 세로 스크롤 · 시스템 바 · 키보드 인셋만큼 비켜 입력 칸이 가려지지 않음.
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
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.apply {
                setWindowAnimations(0)
                // 창을 화면 전체로 — 덮개(검은색 60%)를 Compose 가 직접 그림. AAOS 창 관리자는 창 dim 을 무시하므로 창 dim 은 끔(이중으로 어두워지지 않게).
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0f)
                @Suppress("DEPRECATION") // 키보드가 뜨면 ime 인셋이 오도록 — 입력 칸이 키보드에 가려지지 않게.
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            // 덮개 — 화면 전체 · 누름을 삼키고 아무 일도 하지 않음(사람 결정: 바깥을 눌러도 안 닫힘 · 닫기는 왼쪽 버튼 · 시스템 뒤로).
            // clickable 대신 pointerInput — clickable 은 안의 글자 의미 정보를 합쳐 버튼 찾기를 흐림.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = TravelCrumbsColors.BackgroundDimAlpha))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(APP_DIALOG_SCRIM_TAG),
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
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
