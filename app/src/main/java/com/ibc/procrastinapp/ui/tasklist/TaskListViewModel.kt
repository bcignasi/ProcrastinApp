/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */

// TaskListViewModel.kt
package com.ibc.procrastinapp.ui.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest


/**
 * ViewModel para la pantalla de lista de tareas
 *
 * El ViewModel se ha refactorizado para usar un estado único (UiState)
 * que contiene toda la información necesaria para la UI.
 *
 * Ventajas:
 * - Reduce la complejidad al tener un único estado
 * - Facilita testing
 * - Evita estados inconsistentes
 */
class TaskListViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    // NO NECESITAMOS ESTE ESTADO PARA LAS CARGAS DESDE ROOM, QUE SON INSTANTÁNEAS
//    // 🔹 Este flujo representa si la pantalla está cargando tareas
//    private val _isLoading = MutableStateFlow(true)

    // 🔹 Aquí se almacenan los ID de las tareas que el usuario ha seleccionado
    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())

    // 🔹 Mensaje de error o confirmación (es null cuando no hay mensaje)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // 🔹 Indica si debe mostrarse el diálogo de acciones (completar/eliminar)
    private val _isActionsDialogVisible = MutableStateFlow(false)

    // Estado para el filtro actual
    private val _currentFilter = MutableStateFlow(TaskListQueryType.ALL)

    /**
     * 🔹 Este `StateFlow` se alimenta directamente del `Flow` que devuelve el repositorio
     * y se convierte en un `StateFlow` que Compose puede observar.
     *
     * Usamos `stateIn()` para:
     * - mantener el último valor emitido
     * - no tener que hacer manualmente `_tasks.value = ...`
     * - evitar errores al inicializar
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val tasks: StateFlow<List<Task>> = _currentFilter
        .flatMapLatest { filter ->
            taskRepository.getTasks(filter)
        }
        .stateIn(
            scope = viewModelScope, // ← Se cancela automáticamente si se destruye el ViewModel
            started = SharingStarted.WhileSubscribed(5000), // ← Solo activo cuando hay observadores
            initialValue = emptyList() // ← Valor por defecto hasta que se cargue algo
        )

    // Estado combinado para la UI
    val uiState: StateFlow<TaskListUiState> = combine(
        tasks,
        _selectedTaskIds,
        _errorMessage,
        _isActionsDialogVisible
    ) { tasks, selectedTaskIds, errorMessage, isActionsDialogVisible ->
        TaskListUiState(
            tasks = tasks,
            selectedTaskIds = selectedTaskIds,
            errorMessage = errorMessage,
            isActionsDialogVisible = isActionsDialogVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState(/*isLoading = true*/)
    )

    /**
     * Actualiza el filtro de tareas actual
     */
    fun setTaskFilter(filter: TaskListQueryType) {
        _currentFilter.value = filter
    }

    /**
     * Alterna la selección de una tarea
     *
     * @param taskId ID de la tarea a alternar
     */
    fun toggleTaskSelection(taskId: Long) {
        _selectedTaskIds.update { currentSelection ->
            if (taskId in currentSelection) {
                currentSelection - taskId
            } else {
                currentSelection + taskId
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            // Mejor así; funciona como un toggle
            taskRepository.setTaskCompleted(task.id, !task.completed)
//            taskRepository.completeTasks(listOf(task.id))
            clearSelections()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTasks(listOf(task.id))
            clearSelections()
        }
    }


    /**
     * Completa las tareas seleccionadas
     */
    fun completeSelectedTasks() {
        viewModelScope.launch {
            try {
                val taskIds = _selectedTaskIds.value
                taskRepository.completeTasks(taskIds.toList())
                _selectedTaskIds.value = emptySet()
                _isActionsDialogVisible.value = false
                _errorMessage.value = "Tareas completadas correctamente"
            } catch (e: Exception) {
                _errorMessage.value = "Error al completar tareas: ${e.message}"
            }
        }
    }

    /**
     * Elimina las tareas seleccionadas
     */
    fun deleteSelectedTasks() {
        viewModelScope.launch {
            try {
                val taskIds = _selectedTaskIds.value
                taskRepository.deleteTasks(taskIds.toList())
                _selectedTaskIds.value = emptySet()
                _isActionsDialogVisible.value = false
                _errorMessage.value = "Tareas eliminadas correctamente"
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar tareas: ${e.message}"
            } finally {
                delay(3000)
                _errorMessage.value = null
            }
        }
    }

    /**
     * Muestra el diálogo de acciones
     */
    fun showActionsDialog() {
        _isActionsDialogVisible.value = true
    }

    // SIN USO ACTUALMENTE; SE ACCIONA DIRECTAMENTE EN EL CÓDIGO
//    /**
//     * Oculta el diálogo de acciones
//     */
//    fun hideActionsDialog() {
//        _isActionsDialogVisible.value = false
//    }

    /**
     * Limpia todas las selecciones
     */
    fun clearSelections() {
        _selectedTaskIds.value = emptySet()
        _isActionsDialogVisible.value = false
    }

}