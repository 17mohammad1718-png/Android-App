package com.dataguard.app.presentation.screens.datacap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataguard.app.R
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.NetworkType
import com.dataguard.app.domain.usecase.ObserveCapUseCase
import com.dataguard.app.domain.usecase.SaveCapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val GB: Double = 1024.0 * 1024.0 * 1024.0

@HiltViewModel
class DataCapViewModel @Inject constructor(
    private val observeCap: ObserveCapUseCase,
    private val saveCap: SaveCapUseCase,
) : ViewModel() {

    private var cycleDayState by mutableStateOf(1)
    val cycleDay: Int get() = cycleDayState
    private var limitTextState by mutableStateOf("10")
    val limitText: String get() = limitTextState
    private var thresholdState by mutableStateOf(80)
    val threshold: Int get() = thresholdState
    private var networkTypeState by mutableStateOf(NetworkType.MOBILE)
    val networkType: NetworkType get() = networkTypeState
    private var savedState by mutableStateOf(false)
    val saved: Boolean get() = savedState
    private var invalidLimitState by mutableStateOf(false)
    val invalidLimit: Boolean get() = invalidLimitState
    private var saveFailedState by mutableStateOf(false)
    val saveFailed: Boolean get() = saveFailedState

    init {
        viewModelScope.launch {
            observeCap().first()?.let { c ->
                cycleDayState = c.cycleStartDay
                limitTextState = String.format(Locale.US, "%.0f", c.monthlyLimitBytes / GB)
                thresholdState = c.alertThresholdPercent
                networkTypeState = c.networkType
            }
        }
    }

    fun setCycleDay(v: Int) {
        cycleDayState = v
        savedState = false
        saveFailedState = false
    }

    fun setLimitText(v: String) {
        limitTextState = v
        savedState = false
        saveFailedState = false
        invalidLimitState = false
    }

    fun setThreshold(v: Int) {
        thresholdState = v
        savedState = false
        saveFailedState = false
    }

    fun setNetworkType(v: NetworkType) {
        networkTypeState = v
        savedState = false
        saveFailedState = false
    }

    fun save() {
        val gb = limitTextState.toDoubleOrNull()
        if (gb == null || gb <= 0) {
            invalidLimitState = true
            return
        }
        val bytes = (gb * GB).toLong()
        viewModelScope.launch {
            try {
                saveCap(DataCap(cycleDayState, bytes, thresholdState, networkTypeState))
                savedState = true
            } catch (_: Exception) {
                saveFailedState = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCapScreen(viewModel: DataCapViewModel = hiltViewModel()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.cap_cycle_day),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.cap_cycle_day_value, viewModel.cycleDay),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = viewModel.cycleDay.toFloat(),
            onValueChange = { viewModel.setCycleDay(it.toInt()) },
            valueRange = 1f..31f,
            steps = 29,
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.limitText,
            onValueChange = viewModel::setLimitText,
            label = { Text(stringResource(R.string.cap_limit_gb)) },
            isError = viewModel.invalidLimit,
            supportingText = {
                if (viewModel.invalidLimit) {
                    Text(stringResource(R.string.cap_invalid_limit))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.cap_threshold),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.cap_threshold_value, viewModel.threshold),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = viewModel.threshold.toFloat(),
            onValueChange = { viewModel.setThreshold(it.toInt()) },
            valueRange = 50f..100f,
            steps = 9,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.cap_network),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf(
                NetworkType.MOBILE to stringResource(R.string.cap_network_mobile),
                NetworkType.WIFI to stringResource(R.string.cap_network_wifi),
                NetworkType.BOTH to stringResource(R.string.cap_network_both),
            )
            options.forEachIndexed { index, (type, label) ->
                SegmentedButton(
                    selected = viewModel.networkType == type,
                    onClick = { viewModel.setNetworkType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) {
                    Text(label)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cap_save))
        }
        if (viewModel.saved) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cap_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (viewModel.saveFailed) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.common_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
