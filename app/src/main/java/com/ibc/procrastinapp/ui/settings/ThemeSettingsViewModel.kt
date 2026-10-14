package com.ibc.procrastinapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibc.procrastinapp.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class ThemeSettingsState(
    val darkTheme: Boolean? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false
)

class ThemeSettingsViewModel(private val preferences: ThemePreferences) : ViewModel() {
    private val _uiState = MutableStateFlow(ThemeSettingsState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.darkTheme.collect { darkTheme ->
                _uiState.update { it.copy(darkTheme = darkTheme, isLoading = false) }
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        if (_uiState.value.isLoading || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }

        viewModelScope.launch {
            try {
                preferences.setDarkTheme(enabled)
                _uiState.update { it.copy(darkTheme = enabled) }
            } catch (_: IOException) {
                _uiState.update { it.copy(saveFailed = true) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
