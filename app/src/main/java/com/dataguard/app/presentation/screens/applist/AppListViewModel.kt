package com.dataguard.app.presentation.screens.applist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.usecase.GetAppUsageUseCase
import com.dataguard.app.domain.util.DateUtils
import com.dataguard.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class AppListUiState(
    val period: UsagePeriod = UsagePeriod.DAY,
    val items: List<AppUsage> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val getAppUsage: GetAppUsageUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "AppListViewModel"
    }

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState

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
                Result.runCatching {
                    val start = when (period) {
                        UsagePeriod.DAY -> DateUtils.startOfDayMillis()
                        UsagePeriod.WEEK -> DateUtils.startOfDayMillis(LocalDate.now().minusDays(6))
                        UsagePeriod.MONTH -> DateUtils.startOfDayMillis(LocalDate.now().minusDays(29))
                    }
                    getAppUsage(start, System.currentTimeMillis())
                }
            }
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        items = result.data,
                        loading = false,
                        error = false,
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "Load failed", result.exception)
                    _uiState.value = _uiState.value.copy(
                        items = emptyList(),
                        loading = false,
                        error = true,
                    )
                }
            }
        }
    }
}
