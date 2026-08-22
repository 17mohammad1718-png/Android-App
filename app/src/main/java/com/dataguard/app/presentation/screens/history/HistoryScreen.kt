package com.dataguard.app.presentation.screens.history

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dataguard.app.R
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.usecase.GetHistoryUseCase
import com.dataguard.app.domain.util.ByteFormatter
import com.dataguard.app.presentation.screens.applist.PeriodSelector
import com.dataguard.app.presentation.theme.MobileColor
import com.dataguard.app.presentation.theme.WifiColor
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistory: GetHistoryUseCase,
) : ViewModel() {

    var period by mutableStateOf(UsagePeriod.WEEK)
        private set

    private val _points = MutableStateFlow<List<HistoryPoint>>(emptyList())
    val points: StateFlow<List<HistoryPoint>> = _points

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        load()
    }

    fun setPeriod(newPeriod: UsagePeriod) {
        if (period == newPeriod) return
        period = newPeriod
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _points.value = getHistory(period)
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val points by viewModel.points.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PeriodSelector(
            selected = viewModel.period,
            options = listOf(
                UsagePeriod.WEEK to stringResource(R.string.history_period_week),
                UsagePeriod.MONTH to stringResource(R.string.history_period_month),
            ),
            onSelect = viewModel::setPeriod,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            loading -> {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            points.isEmpty() -> {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            else -> {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    UsageChart(
                        points = points,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    TotalRow(
                        label = stringResource(R.string.dashboard_total),
                        value = ByteFormatter.format(points.sumOf { it.totalBytes }),
                    )
                    LegendRow(
                        wifi = ByteFormatter.format(points.sumOf { it.wifiBytes }),
                        mobile = ByteFormatter.format(points.sumOf { it.mobileBytes }),
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LegendRow(wifi: String, mobile: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(WifiColor, stringResource(R.string.dashboard_wifi), wifi)
        LegendItem(MobileColor, stringResource(R.string.dashboard_mobile), mobile)
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A simple Vico column chart of per-day totals.
 * x = day index, y = total bytes; the bottom axis maps index -> date label.
 */
@Composable
fun UsageChart(points: List<HistoryPoint>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(points) { points.map { it.label } }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnSeries {
                series(
                    x = points.indices.map { it.toDouble() },
                    y = points.map { it.totalBytes.toDouble() },
                )
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    ByteFormatter.format(value.toLong())
                },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    val idx = value.toInt()
                    labels.getOrElse(idx) { "" }
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(260.dp),
    )
}
