package com.example.travelcrumbs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcrumbs.R
import com.example.travelcrumbs.ui.theme.Dimens

@Composable
fun ScreenHeader(title: String, parked: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(Dimens.ScreenHeaderHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        Surface(
            modifier = Modifier.width(261.dp).height(84.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (parked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            contentColor = if (parked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            border = BorderStroke(2.dp, if (parked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
        ) {
            Text(
                text = stringResource(if (parked) R.string.vehicle_state_parked else R.string.vehicle_state_driving),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
    }
}
