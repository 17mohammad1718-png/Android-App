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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dataguard.app.R
import com.dataguard.app.domain.model.NetworkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCapScreen(viewModel: DataCapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            text = stringResource(R.string.cap_cycle_day_value, state.cycleDay),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = state.cycleDay.toFloat(),
            onValueChange = { viewModel.setCycleDay(it.toInt()) },
            valueRange = 1f..31f,
            steps = 29,
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.limitText,
            onValueChange = viewModel::setLimitText,
            label = { Text(stringResource(R.string.cap_limit_gb)) },
            isError = state.invalidLimit,
            supportingText = {
                if (state.invalidLimit) {
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
            text = stringResource(R.string.cap_threshold_value, state.threshold),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = state.threshold.toFloat(),
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
                    selected = state.networkType == type,
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
        if (state.saved) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cap_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (state.saveFailed) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.common_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
