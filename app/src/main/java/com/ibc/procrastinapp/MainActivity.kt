/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibc.procrastinapp.ui.navigation.AppNavigation
import com.ibc.procrastinapp.ui.settings.ThemeSettingsViewModel
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeSettingsViewModel = koinViewModel()
            val settings by themeViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = settings.darkTheme ?: isSystemInDarkTheme()

            ProcAppMVVMTheme(darkTheme = darkTheme) {
                // Un contenedor Surface usando el color de fondo del tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Nuestro composable de navegación
                    if (!settings.isLoading) {
                        AppNavigation(
                            themeSettings = settings,
                            onDarkThemeChange = themeViewModel::setDarkTheme
                        )
                    }
                }
            }
        }
    }
}

