package com.dataguard.app.presentation.screens.datacap

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.NetworkType
import com.dataguard.app.domain.usecase.ObserveCapUseCase
import com.dataguard.app.domain.usecase.SaveCapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val GB: Double = 1024.0 * 1024.0 * 1024.0

data class DataCapUiState(
    val cycleDay: Int = 1,
    val limitText: String = "10",
    val threshold: Int = 80,
    val networkType: NetworkType = NetworkType.MOBILE,
    val saved: Boolean = false,
    val invalidLimit: Boolean = false,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class DataCapViewModel @Inject constructor(
    private val observeCap: ObserveCapUseCase,
    private val saveCap: SaveCapUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "DataCapViewModel"
    }

    private val _uiState = MutableStateFlow(DataCapUiState())
    val uiState: StateFlow<DataCapUiState> = _uiState

    init {
        viewModelScope.launch {
            observeCap().first()?.let { c ->
                _uiState.value = DataCapUiState(
                    cycleDay = c.cycleStartDay,
                    limitText = String.format(Locale.US, "%.0f", c.monthlyLimitBytes / GB),
                    threshold = c.alertThresholdPercent,
                    networkType = c.networkType,
                )
            }
        }
    }

    fun setCycleDay(v: Int) {
        _uiState.value = _uiState.value.copy(cycleDay = v, saved = false, saveFailed = false)
    }

    fun setLimitText(v: String) {
        _uiState.value = _uiState.value.copy(
            limitText = v,
            saved = false,
            saveFailed = false,
            invalidLimit = false,
        )
    }

    fun setThreshold(v: Int) {
        _uiState.value = _uiState.value.copy(threshold = v, saved = false, saveFailed = false)
    }

    fun setNetworkType(v: NetworkType) {
        _uiState.value = _uiState.value.copy(networkType = v, saved = false, saveFailed = false)
    }

    fun save() {
        val state = _uiState.value
        val gb = state.limitText.toDoubleOrNull()
        if (gb == null || gb <= 0) {
            _uiState.value = state.copy(invalidLimit = true)
            return
        }
        val bytes = (gb * GB).toLong()
        viewModelScope.launch {
            try {
                saveCap(
                    DataCap(
                        cycleStartDay = state.cycleDay,
                        monthlyLimitBytes = bytes,
                        alertThresholdPercent = state.threshold,
                        networkType = state.networkType,
                    ),
                )
                _uiState.value = _uiState.value.copy(saved = true)
            } catch (e: Exception) {
                Log.e(TAG, "Save failed", e)
                _uiState.value = _uiState.value.copy(saveFailed = true)
            }
        }
    }
}
