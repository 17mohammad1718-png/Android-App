package com.dataguard.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataguard.app.domain.model.AppSettings
import com.dataguard.app.domain.model.DisplayUnit
import com.dataguard.app.domain.model.ThemeMode
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val usageRepository: DataUsageRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess

    fun refresh() {
        _hasUsageAccess.value = usageRepository.hasUsageAccess()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDisplayUnit(unit: DisplayUnit) {
        viewModelScope.launch { settingsRepository.setDisplayUnit(unit) }
    }
}
