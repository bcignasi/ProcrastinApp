/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// TaskListScreen.kt
package com.ibc.procrastinapp.ui.tasklist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.tasklist.elements.AddTaskFab
import com.ibc.procrastinapp.ui.tasklist.elements.EmptyListMessage
import com.ibc.procrastinapp.ui.tasklist.elements.TaskListContent
import com.ibc.procrastinapp.ui.tasklist.elements.TaskListTopBar
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla que muestra la lista de tareas guardadas
 *
 * La pantalla se ha refactorizado siguiendo los siguientes principios:
 * 1. Separación de responsabilidades: Cada componente tiene una única responsabilidad
 * 2. Elevación de estado: El estado se gestiona en el ViewModel y se pasa a los componentes
 * 3. Componentización: La UI se divide en componentes reutilizables
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = koinViewModel()
) {
    // Recolectamos todos los estados del ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Estado para controlar qué lista se muestra (inicialmente, todas las tareas)
    var currentListType by remember { mutableStateOf(TaskListQueryType.ALL) }

    // Efecto para cargar las tareas según el tipo de lista
    LaunchedEffect(currentListType) {
        viewModel.setTaskFilter(currentListType)
    }

    // Estado para detectar si el botón atrás ya fue pulsado una vez recientemente
    var backPressedOnce by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Si está activo, lanzamos un efecto que lo desactiva tras 2 segundos
    if (backPressedOnce) {
        LaunchedEffect(Unit) {
            delay(2000)
            backPressedOnce = false
        }
    }


    // Interceptamos el botón "atrás" del sistema
    BackHandler {
        if (backPressedOnce) {
            // Si el usuario ya pulsó una vez, se cierra la app forzadamente
            android.os.Process.killProcess(android.os.Process.myPid())
        } else {
            // Primera pulsación: mostramos un mensaje y activamos el modo "esperando segunda pulsación"
            backPressedOnce = true

            Toast.makeText(
                context,
                context.getString(R.string.tasklist_back_press_to_exit),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

///////////////////////////////////////////////////////////////////////////////

    Scaffold(
        topBar = {
            TaskListTopBar(
                onGoToAssistant = { navController.navigate("chat") },
                currentListType = currentListType,
                onCurrentListTypeChange = { newType -> // Proporciona la lógica para cambiar el estado
                    currentListType = newType
                }
            )
        },

        floatingActionButton = {
            AddTaskFab(onClick = {
                // ==== NAVEGACIÓN ON CLICK =================================
                navController.navigate("chat?initSession=true")
                // ==========================================================
            })
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)     // Necesario para que el contenido del Box respete al Scaffold
        ) {
            // Presentamos el contenido según el estado actual
            when {
                // NO NECESITAMOS ESTE ESTADO SI CARGAMOS DESDE ROOM
//                uiState.isLoading -> LoadingIndicator(Modifier.align(Alignment.Center))

                uiState.tasks.isEmpty() ->
                    EmptyListMessage(Modifier.align(Alignment.Center))

                else ->
                    TaskListContent(
                        uiState = uiState,
                        onTaskClick = { taskId ->
                            if (uiState.selectedTaskIds.isNotEmpty()) {
                                viewModel.toggleTaskSelection(taskId)
                            } else {

                                // ==== NAVEGACIÓN ON CLICK PARA EDICIÓN ====================
                                navController.navigate("chat/$taskId")
                                // ==========================================================

                            }
                        },
                        onTaskLongClick = { viewModel.toggleTaskSelection(it) },
                        onCompleteTasksClick = { viewModel.completeSelectedTasks() },
                        onDeleteTasksClick = { viewModel.deleteSelectedTasks() },
                        onEditTasksClick = {
                            val selectedTasksIds = uiState.selectedTaskIds.joinToString(",")
                            Logger.d("IBC11-TaskListScreen", "onEditTasksClick: $selectedTasksIds")
                            navController.navigate("chat/$selectedTasksIds")
                        },
                        onDismissDialog = {
                            viewModel.clearSelections()
                            //viewModel.hideActionsDialog()  // Está incluido en clearSelections
                        },
                        onActionsClick = { viewModel.showActionsDialog() },
                        onCompleteSingleTask = { task: Task -> viewModel.completeTask(task) },
                        onDeleteSingleTask = { task: Task -> viewModel.deleteTask(task) }
                    )
            }
        }
    }
}

// NO NECESITAMOS INDICADOR DE CARGA EN LAS CARGAS DE ROOM
///**
// * Indicador de carga
// */
//@Composable
//private fun LoadingIndicator(modifier: Modifier = Modifier) {
//    CircularProgressIndicator(modifier = modifier)
//}

