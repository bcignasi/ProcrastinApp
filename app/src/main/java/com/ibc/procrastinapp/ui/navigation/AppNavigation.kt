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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// Recursos de la aplicación (vídeo de Splash)
import com.ibc.procrastinapp.R

// Pantallas de la aplicación
import com.ibc.procrastinapp.ui.assistant.AssistantScreen
import com.ibc.procrastinapp.ui.splash.SplashScreen
import com.ibc.procrastinapp.ui.tasklist.TaskListScreen

// Utilidades
import kotlinx.coroutines.delay

/**
 * Configuración de navegación principal de la aplicación.
 * Define las rutas y la navegación entre las diferentes pantallas.
 */
@Composable
fun AppNavigation() {

    // Crea y recuerda una instancia del controlador de navegación
    // Se mantiene durante recomposiciones
    val navController = rememberNavController()

    // Estado local para controlar la finalización del video splash
    // remember + mutableStateOf mantiene el estado durante recomposiciones
    var videoCompleted by remember { mutableStateOf(false) }

    // NavHost: Contenedor principal del grafo de navegación
    NavHost(
        navController = navController, // Controlador que gestiona la navegación

        startDestination = "splash"    // Ruta inicial de la aplicación
                                        // TODO: flag para ejecutar splash o no
    ) {
        // Definición de la ruta de navegación splash
        composable("splash") {

            // Composable que muestra la pantalla de splash
            SplashScreen(
                videoResId = R.raw.splash_video,
                onVideoComplete = {  // Callback cuando el video termina
                    // Navegar inmediatamente cuando se complete el video (por click o finalización natural)
                    navController.navigate("task_list") {
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
                    navController.navigate("task_list") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

        // Ruta simple para chat sin parámetros
        composable("chat") {
            AssistantScreen(
                navController = navController,
            )
        }

        // Ruta parametrizada con query parameter opcional para iniciar una nueva sesión
        composable(
            route = "chat?initSession={initSession}",
            arguments = listOf(navArgument("initSession") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val initSession = backStackEntry.arguments?.getBoolean("initSession") == true
            AssistantScreen(
                navController = navController,
                initSession = initSession,
            )
        }

        // Pantalla de chat con tareas para editar
        composable(
            route = "chat/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskIdParam = backStackEntry.arguments?.getString("taskId")
            val taskIds = taskIdParam?.toTaskIdList() ?: emptyList()

            AssistantScreen(
                navController = navController,
                taskIdsToEdit = taskIds,
            )
        }

        // Pantalla de lista de tareas
        composable("task_list") {
            TaskListScreen(
                navController = navController
            )
        }
    }
}
