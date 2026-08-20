package com.dataguard.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.util.ByteFormatter
import com.dataguard.app.presentation.theme.MobileColor
import com.dataguard.app.presentation.theme.WifiColor

/** Horizontal bar showing the Wi-Fi vs mobile split. */
@Composable
fun UsageBreakdownBar(
    wifiBytes: Long,
    mobileBytes: Long,
    modifier: Modifier = Modifier,
) {
    val total = (wifiBytes + mobileBytes).coerceAtLeast(1)
    val wifiFraction = (wifiBytes.toFloat() / total).coerceIn(0f, 1f)
    val mobileFraction = 1f - wifiFraction

    Row(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
    ) {
        if (wifiFraction > 0f) {
            Box(
                Modifier
                    .weight(wifiFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(WifiColor),
            )
        }
        if (mobileFraction > 0f) {
            Box(
                Modifier
                    .weight(mobileFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(MobileColor),
            )
        }
    }
}

@Composable
fun AppUsageRow(
    app: AppUsage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ByteFormatter.format(app.totalBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UsageBreakdownBar(app.wifiBytes, app.mobileBytes, Modifier.padding(top = 6.dp))
        }
    }
}
