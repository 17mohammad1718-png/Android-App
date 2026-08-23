package com.dataguard.app.presentation.screens.dashboard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dataguard.app.R
import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.presentation.components.UsageBreakdownBar
import com.dataguard.app.presentation.components.formatBytes
import com.dataguard.app.presentation.theme.MobileColor
import com.dataguard.app.presentation.theme.WifiColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCap: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS on Android 13+ (needed for data cap alerts)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied — handled silently */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !uiState.hasAccess -> {
                    item {
                        PermissionCard {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                }
                uiState.error != null -> {
                    item { ErrorCard(onRetry = viewModel::refresh) }
                }
                else -> {
                    item { TodayUsageCard(uiState.todayUsage) }

                    item {
                        CapCard(
                            progress = uiState.capProgress,
                            onOpenCap = onOpenCap,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayUsageCard(today: TodayUsage?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_today),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (today == null) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else {
                Text(
                    text = formatBytes(today.totalBytes),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                UsageBreakdownBar(today.wifiBytes, today.mobileBytes)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NetworkStat(
                        label = stringResource(R.string.dashboard_wifi),
                        value = formatBytes(today.wifiBytes),
                        color = WifiColor,
                        modifier = Modifier.weight(1f),
                    )
                    NetworkStat(
                        label = stringResource(R.string.dashboard_mobile),
                        value = formatBytes(today.mobileBytes),
                        color = MobileColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun CapCard(
    progress: CapProgress?,
    onOpenCap: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_cap),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (progress == null) {
                Text(
                    text = stringResource(R.string.dashboard_cap_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenCap) {
                    Text(stringResource(R.string.dashboard_cap_edit))
                }
            } else {
                LinearProgressIndicator(
                    progress = { progress.percent },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dashboard_cap_used) + " " +
                        formatBytes(progress.usedBytes) + " / " +
                        formatBytes(progress.cap.monthlyLimitBytes),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.dashboard_cap_remaining) + " " +
                        formatBytes(progress.remainingBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progress.predictedEndMillis?.let { predicted ->
                    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                    val date = Instant.ofEpochMilli(predicted)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                    Text(
                        text = stringResource(R.string.dashboard_cap_predicted, date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenCap) {
                    Text(stringResource(R.string.dashboard_cap_edit))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_permission_missing),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.dashboard_open_settings))
            }
        }
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.common_error),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}
