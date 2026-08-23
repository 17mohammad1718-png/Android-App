package com.dataguard.app.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import com.dataguard.app.domain.repository.DataUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val usageRepository: DataUsageRepository,
) : ViewModel() {

    private val _granted = MutableStateFlow(false)
    val granted: StateFlow<Boolean> = _granted

    fun refresh() {
        _granted.value = usageRepository.hasUsageAccess()
    }
}
