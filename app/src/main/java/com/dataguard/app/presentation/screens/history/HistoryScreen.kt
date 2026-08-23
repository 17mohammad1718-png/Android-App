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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dataguard.app.R
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.util.ByteFormatter
import com.dataguard.app.presentation.components.formatBytes
import com.dataguard.app.presentation.screens.applist.PeriodSelector
import com.dataguard.app.presentation.theme.LocalDisplayUnit
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PeriodSelector(
            selected = uiState.period,
            options = listOf(
                UsagePeriod.WEEK to stringResource(R.string.history_period_week),
                UsagePeriod.MONTH to stringResource(R.string.history_period_month),
            ),
            onSelect = viewModel::setPeriod,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            uiState.loading -> {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
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
            uiState.points.isEmpty() -> {
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
                        points = uiState.points,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    TotalRow(
                        label = stringResource(R.string.dashboard_total),
                        value = formatBytes(uiState.points.sumOf { it.totalBytes }),
                    )
                    LegendRow(
                        wifi = formatBytes(uiState.points.sumOf { it.wifiBytes }),
                        mobile = formatBytes(uiState.points.sumOf { it.mobileBytes }),
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
    color: Color,
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
    val displayUnit = LocalDisplayUnit.current

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
                    ByteFormatter.format(value.toLong(), displayUnit)
                },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    val idx = value.roundToInt()
                    labels.getOrElse(idx) { "" }
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(260.dp),
    )
}
