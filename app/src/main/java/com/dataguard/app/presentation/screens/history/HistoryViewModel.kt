package com.dataguard.app.presentation.screens.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.usecase.GetHistoryUseCase
import com.dataguard.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HistoryUiState(
    val period: UsagePeriod = UsagePeriod.WEEK,
    val points: List<HistoryPoint> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistory: GetHistoryUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "HistoryViewModel"
    }

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        load()
    }

    fun setPeriod(newPeriod: UsagePeriod) {
        if (_uiState.value.period == newPeriod) return
        _uiState.value = _uiState.value.copy(period = newPeriod)
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = false)
            val period = _uiState.value.period
            val result = withContext(Dispatchers.IO) {
                Result.runCatching { getHistory(period) }
            }
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        points = result.data,
                        loading = false,
                        error = false,
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "Load failed", result.exception)
                    _uiState.value = _uiState.value.copy(
                        points = emptyList(),
                        loading = false,
                        error = true,
                    )
                }
            }
        }
    }
}
