package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.ui.theme.TcColors

@Composable
internal fun DemoPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("TRAVEL CRUMBS  /  시연 모드", style = MaterialTheme.typography.labelMedium, color = TcColors.BrandSecondary)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TcColors.TextSecondary)
        content()
    }
}

@Composable
internal fun DemoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun DemoTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun DemoAction(text: String, onClick: () -> Unit, enabled: Boolean = true, primary: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    val modifier = Modifier.heightIn(min = 64.dp)
    if (primary) {
        Button(onClick, modifier, enabled, shape) { Text(text) }
    } else {
        OutlinedButton(onClick, modifier, enabled, shape) { Text(text) }
    }
}

@Composable
internal fun DemoConfirm(title: String, message: String, action: String, onClose: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { DemoTitle(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { DemoAction(action, onConfirm) },
        dismissButton = { DemoAction("취소", onClose, primary = false) },
    )
}

@Composable
internal fun DemoLogin(onLogin: (String) -> Unit) {
    DemoPage("여행의 흔적을 남겨요", "시연 계정을 선택하면 샘플 여행을 기록하고 추천에 공개할 수 있어요.") {
        DemoCard {
            DemoTitle("시연 계정")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DemoAction("지현으로 로그인", { onLogin("지현") })
                DemoAction("준호로 로그인", { onLogin("준호") }, primary = false)
            }
        }
    }
}
