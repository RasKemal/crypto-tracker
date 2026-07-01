package com.example.stocktracker.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository,
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = preferences.observeIsDarkTheme()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun toggleTheme() {
        viewModelScope.launch {
            preferences.setDarkTheme(!isDarkTheme.value)
        }
    }
}
