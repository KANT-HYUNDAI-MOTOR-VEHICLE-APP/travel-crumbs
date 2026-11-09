package com.example.travelcrumbs.testing

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository

const val FAKE_INPUT_TAG = "fake_input"
const val FAKE_INPUT_FIELD = "memo"

/**
 * 시험 전용 가짜 입력 화면(스펙 app-shell D5) — 테스트 소스에만 · 앱 화면 아님.
 * 실제 입력 화면(#3)이 작성 중 내용 연결 지점(D27)을 쓰는 방법 그대로: 입력마다 알리고 · 복원 상태를 받아 쓰고 지움.
 */
@Composable
fun FakeInputContent(
    inputs: UnfinishedInputRepository,
    destinationId: String,
) {
    var text by remember { mutableStateOf("") }
    val restore by inputs.pendingRestore.collectAsState()

    LaunchedEffect(restore) {
        val pending = restore
        if (pending != null && pending.destinationId == destinationId) {
            text = pending.fields[FAKE_INPUT_FIELD].orEmpty()
            inputs.updateActiveInput(destinationId, pending.fields)
            inputs.consumeRestore(destinationId)
        }
    }

    TextField(
        value = text,
        onValueChange = {
            text = it
            inputs.updateActiveInput(destinationId, mapOf(FAKE_INPUT_FIELD to it))
        },
        modifier = Modifier.testTag(FAKE_INPUT_TAG),
    )
}
