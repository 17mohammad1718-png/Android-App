package com.dataguard.app.presentation.screens.dashboard

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dataguard.app.R
import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.usecase.ComputeCapProgressUseCase
import com.dataguard.app.domain.usecase.GetTodayUsageUseCase
import com.dataguard.app.domain.usecase.RefreshDataUseCase
import com.dataguard.app.domain.util.ByteFormatter
import com.dataguard.app.presentation.components.UsageBreakdownBar
import com.dataguard.app.presentation.theme.MobileColor
import com.dataguard.app.presentation.theme.WifiColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: DataUsageRepository,
    private val getTodayUsage: GetTodayUsageUseCase,
    private val computeCapProgress: ComputeCapProgressUseCase,
    private val refreshData: RefreshDataUseCase,
) : ViewModel() {

    private val _todayUsage = MutableStateFlow<TodayUsage?>(null)
    val todayUsage: StateFlow<TodayUsage?> = _todayUsage

    private val _capProgress = MutableStateFlow<CapProgress?>(null)
    val capProgress: StateFlow<CapProgress?> = _capProgress

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _hasAccess = MutableStateFlow(true)
    val hasAccess: StateFlow<Boolean> = _hasAccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                _hasAccess.value = usageRepository.hasUsageAccess()
                if (_hasAccess.value) {
                    refreshData()
                    _todayUsage.value = getTodayUsage()
                }
                _capProgress.value = computeCapProgress()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCap: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val today by viewModel.todayUsage.collectAsStateWithLifecycle()
    val capProgress by viewModel.capProgress.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val hasAccess by viewModel.hasAccess.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !hasAccess -> {
                    item {
                        PermissionCard {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                }
                error != null -> {
                    item { ErrorCard(onRetry = viewModel::refresh) }
                }
                else -> {
                    item { TodayUsageCard(today) }

                    item {
                        CapCard(
                            progress = capProgress,
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
                    text = ByteFormatter.format(today.totalBytes),
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
                        value = ByteFormatter.format(today.wifiBytes),
                        color = WifiColor,
                        modifier = Modifier.weight(1f),
                    )
                    NetworkStat(
                        label = stringResource(R.string.dashboard_mobile),
                        value = ByteFormatter.format(today.mobileBytes),
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
    color: androidx.compose.ui.graphics.Color,
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
                    Text(stringResource(R.string.dashboard_cap_set))
                }
            } else {
                LinearProgressIndicator(
                    progress = { progress.percent },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.dashboard_cap_used) + " " +
                        ByteFormatter.format(progress.usedBytes) + " / " +
                        ByteFormatter.format(progress.cap.monthlyLimitBytes),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.dashboard_cap_remaining) + " " +
                        ByteFormatter.format(progress.remainingBytes),
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
                    Text(stringResource(R.string.dashboard_cap_set))
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
