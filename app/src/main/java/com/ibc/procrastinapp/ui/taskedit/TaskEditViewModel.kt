/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val notFound: Boolean = false
)

/**
 * ViewModel para la pantalla de edición clásica (formulario) de una tarea existente.
 * Es independiente del flujo de edición vía asistente de IA.
 */
class TaskEditViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    private var loadedTaskId: Long? = null

    fun loadTask(taskId: Long) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            _uiState.update { it.copy(task = task, isLoading = false, notFound = task == null) }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(task = it.task?.copy(title = title)) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(task = it.task?.copy(notes = notes)) }
    }

    fun toggleCompleted() {
        _uiState.update { state -> state.copy(task = state.task?.let { it.copy(completed = !it.completed) }) }
    }

    fun save(onSaved: () -> Unit) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            taskRepository.updateTask(task)
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
