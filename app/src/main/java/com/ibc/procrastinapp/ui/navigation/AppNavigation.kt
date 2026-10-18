/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// AppNavigation.kt
package com.ibc.procrastinapp.ui.navigation

// Importaciones de Compose para UI declarativa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Importaciones para navegación
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

// Recursos de la aplicación (vídeo de Splash)
import com.ibc.procrastinapp.R

// Pantallas de la aplicación
import com.ibc.procrastinapp.ui.assistant.AssistantScreen
import com.ibc.procrastinapp.ui.splash.SplashScreen
import com.ibc.procrastinapp.ui.tasklist.TaskListScreen
import com.ibc.procrastinapp.ui.settings.SettingsScreen
import com.ibc.procrastinapp.ui.settings.ThemeSettingsState
import com.ibc.procrastinapp.ui.theme.LocalAppDarkTheme

// Utilidades
import kotlinx.coroutines.delay

/**
 * Configuración de navegación principal de la aplicación.
 * Define las rutas y la navegación entre las diferentes pantallas.
 */
@Composable
fun AppNavigation(
    themeSettings: ThemeSettingsState,
    onDarkThemeChange: (Boolean) -> Unit
) {

    // Crea y recuerda una instancia del controlador de navegación
    // Se mantiene durante recomposiciones
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = backStackEntry?.destination?.mainDestination()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Estado local para controlar la finalización del video splash
    // remember + mutableStateOf mantiene el estado durante recomposiciones
    var videoCompleted by remember { mutableStateOf(false) }

    // The outer scaffold reserves space for navigation; each screen keeps its top bar.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedRoute != null && !keyboardVisible) {
                MainBottomBar(
                    selectedRoute = selectedRoute,
                    onSelect = navController::openMainDestination
                )
            }
        }
    ) { contentPadding ->
        // NavHost: Contenedor principal del grafo de navegación
        NavHost(
            navController = navController, // Controlador que gestiona la navegación
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding(),

            startDestination = "splash"    // Ruta inicial de la aplicación
                                            // TODO: flag para ejecutar splash o no
        ) {
            composable("settings") {
                SettingsScreen(
                    darkTheme = LocalAppDarkTheme.current,
                    isSaving = themeSettings.isSaving,
                    saveFailed = themeSettings.saveFailed,
                    onDarkThemeChange = onDarkThemeChange,
                    onBack = { navController.popBackStack() }
                )
            }

            // Definición de la ruta de navegación splash
            composable("splash") {

                // Composable que muestra la pantalla de splash
                SplashScreen(
                    videoResId = R.raw.splash_video,
                    onVideoComplete = {  // Callback cuando el video termina
                        // Navegar inmediatamente cuando se complete el video (por click o finalización natural)
                        navController.navigate(AGENDA_ROUTE) {
                            popUpTo("splash") { inclusive = true }  // Elimina splash del backstack
                        }
                    }
                )

                // Efecto lanzado cuando el video termina
                LaunchedEffect(key1 = videoCompleted) {
                    if (videoCompleted) {
                        // Pequeño delay para transición
                        delay(300)
                        // Como antes
                        navController.navigate(AGENDA_ROUTE) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }

            // All assistant routes share a restorable navigation graph.
            assistantDestinations { taskIds, initSession ->
                AssistantScreen(
                    navController = navController,
                    taskIdsToEdit = taskIds,
                    initSession = initSession,
                )
            }

            // Pantalla de lista de tareas
            composable(AGENDA_ROUTE) {
                TaskListScreen(
                    navController = navController
                )
            }
        }
    }
}
