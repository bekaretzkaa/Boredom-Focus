package com.example.boredomfocus.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class OnboardingViewModel() : ViewModel() {

    private val _navigateToPage = MutableSharedFlow<Int>(0)
    val navigateToPage = _navigateToPage.asSharedFlow()

    fun onNavigateToPageRequested(pageIndex: Int) {
        viewModelScope.launch {
            _navigateToPage.emit(pageIndex)
        }
    }
}