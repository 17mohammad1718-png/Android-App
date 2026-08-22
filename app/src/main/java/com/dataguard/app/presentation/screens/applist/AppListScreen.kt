package com.dataguard.app.presentation.screens.applist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dataguard.app.R
import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.usecase.GetAppUsageUseCase
import com.dataguard.app.domain.util.DateUtils
import com.dataguard.app.presentation.components.AppUsageRow
import com.dataguard.app.presentation.components.formatBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val getAppUsage: GetAppUsageUseCase,
) : ViewModel() {

    private var periodState by mutableStateOf(UsagePeriod.DAY)
    val period: UsagePeriod get() = periodState
    private val _items = MutableStateFlow<List<AppUsage>>(emptyList())
    val items: StateFlow<List<AppUsage>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    init {
        load()
    }

    fun setPeriod(newPeriod: UsagePeriod) {
        if (periodState == newPeriod) return
        periodState = newPeriod
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = false
            try {
                val start = when (period) {
                    UsagePeriod.DAY -> DateUtils.startOfDayMillis()
                    UsagePeriod.WEEK -> DateUtils.startOfDayMillis(LocalDate.now().minusDays(6))
                    UsagePeriod.MONTH -> DateUtils.startOfDayMillis(LocalDate.now().minusDays(29))
                }
                _items.value = getAppUsage(start, System.currentTimeMillis())
            } catch (_: Exception) {
                _error.value = true
            } finally {
                _loading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(viewModel: AppListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var selectedApp by remember { mutableStateOf<AppUsage?>(null) }

    Column(Modifier.fillMaxSize()) {
        PeriodSelector(
            selected = viewModel.period,
            options = listOf(
                UsagePeriod.DAY to stringResource(R.string.apps_period_day),
                UsagePeriod.WEEK to stringResource(R.string.apps_period_week),
                UsagePeriod.MONTH to stringResource(R.string.apps_period_month),
            ),
            onSelect = viewModel::setPeriod,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            loading -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                }
            }
            error -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.common_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::load) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
            items.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.apps_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    items(items, key = { it.uid.toString() + it.packageName }) { app ->
                        AppUsageRow(app = app, onClick = { selectedApp = app })
                    }
                }
            }
        }
    }

    selectedApp?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text(app.appName) },
            text = {
                Column {
                    DetailRow(stringResource(R.string.apps_detail_wifi), formatBytes(app.wifiBytes))
                    DetailRow(stringResource(R.string.apps_detail_mobile), formatBytes(app.mobileBytes))
                    DetailRow(stringResource(R.string.apps_detail_total), formatBytes(app.totalBytes))
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedApp = null }) {
                    Text(stringResource(R.string.apps_close))
                }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selected: UsagePeriod,
    options: List<Pair<UsagePeriod, String>>,
    onSelect: (UsagePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (period, label) ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label)
            }
        }
    }
}
