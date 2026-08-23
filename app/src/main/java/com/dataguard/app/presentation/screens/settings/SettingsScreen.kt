package com.dataguard.app.presentation.screens.settings

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dataguard.app.BuildConfig
import com.dataguard.app.R
import com.dataguard.app.domain.model.DisplayUnit
import com.dataguard.app.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    var ignoresBattery by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        val pm = context.getSystemService(PowerManager::class.java)
        ignoresBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SettingCard(title = stringResource(R.string.settings_theme)) {
            SegmentedChoice(
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                ),
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode,
            )
        }

        SettingCard(title = stringResource(R.string.settings_unit)) {
            SegmentedChoice(
                options = listOf(
                    DisplayUnit.AUTO to stringResource(R.string.settings_unit_auto),
                    DisplayUnit.MB to stringResource(R.string.settings_unit_mb),
                    DisplayUnit.GB to stringResource(R.string.settings_unit_gb),
                ),
                selected = settings.displayUnit,
                onSelect = viewModel::setDisplayUnit,
            )
        }

        SettingCard(title = stringResource(R.string.settings_usage_access)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (hasUsageAccess) R.string.common_granted else R.string.common_not_granted,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) {
                    Text(stringResource(R.string.settings_open))
                }
            }
        }

        SettingCard(title = stringResource(R.string.settings_battery)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (ignoresBattery) R.string.settings_battery_unrestricted
                        else R.string.settings_battery_optimized,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    val pm = context.getSystemService(PowerManager::class.java)
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                            ),
                        )
                    }
                }) {
                    Text(stringResource(R.string.settings_open))
                }
            }
        }

        SettingCard(title = stringResource(R.string.settings_about)) {
            Text(
                text = stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label)
            }
        }
    }
}
