package com.dataguard.app.presentation.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.usecase.ComputeCapProgressUseCase
import com.dataguard.app.domain.usecase.GetTodayUsageUseCase
import com.dataguard.app.domain.usecase.RefreshDataUseCase
import com.dataguard.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DashboardUiState(
    val todayUsage: TodayUsage? = null,
    val capProgress: CapProgress? = null,
    val isRefreshing: Boolean = false,
    val hasAccess: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: DataUsageRepository,
    private val getTodayUsage: GetTodayUsageUseCase,
    private val computeCapProgress: ComputeCapProgressUseCase,
    private val refreshData: RefreshDataUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

            val hasAccess = usageRepository.hasUsageAccess()
            if (!hasAccess) {
                _uiState.value = _uiState.value.copy(
                    hasAccess = false,
                    isRefreshing = false,
                )
                return@launch
            }

            // Perform all blocking I/O on Dispatchers.IO to avoid ANR/jank.
            val result = withContext(Dispatchers.IO) {
                Result.runCatching {
                    refreshData()
                    val today = getTodayUsage()
                    val progress = computeCapProgress()
                    today to progress
                }
            }

            when (result) {
                is Result.Success -> {
                    val (today, progress) = result.data
                    _uiState.value = _uiState.value.copy(
                        hasAccess = true,
                        todayUsage = today,
                        capProgress = progress,
                        isRefreshing = false,
                        error = null,
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "Refresh failed", result.exception)
                    _uiState.value = _uiState.value.copy(
                        hasAccess = true,
                        isRefreshing = false,
                        error = result.errorMessage(),
                    )
                }
            }
        }
    }
}
