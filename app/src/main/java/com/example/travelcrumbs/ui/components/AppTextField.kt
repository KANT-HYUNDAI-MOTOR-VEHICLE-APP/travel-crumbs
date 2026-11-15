package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/** v1.8 공통 입력 필드. 입력 길이가 늘어나도 글자 크기를 줄이지 않는다. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    errorText: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { text -> { Text(text, style = MaterialTheme.typography.bodyMedium) } },
        placeholder = placeholder?.let { text ->
            { Text(text, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant) }
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        supportingText = errorText?.let { text ->
            { Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        },
        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            focusedLabelColor = colors.primary,
            cursorColor = colors.primary,
            errorBorderColor = colors.error,
            errorLabelColor = colors.error,
            errorCursorColor = colors.error,
        ),
    )
}
