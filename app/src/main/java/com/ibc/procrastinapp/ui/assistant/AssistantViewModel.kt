/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.data.service.AssistantResponseParserImpl
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.data.service.TaskJsonExtractor
import com.ibc.procrastinapp.ui.assistant.AssistantState.ViewModelInfo
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val chatAIService: ChatAIService,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskJsonExtractor = TaskJsonExtractor()
    private val responseParser = AssistantResponseParserImpl()

    // Mantiene la relación de tareas presente en AssistantScreen
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    // Muestra mensajes de confirmación o error
    private val _viewModelInfo = MutableStateFlow<ViewModelInfo?>(null)

    // Señaliza el final de la tarea asíncrona de guardado de tareas
    private val _commitFinished = MutableSharedFlow<Unit>()
    val commitFinished = _commitFinished.asSharedFlow()

    // En el modo edición, la lista de tareas a editar (para borrarlas
    // en caso de que se acepten los cambios - se insertarán como nuevas)
    private val originalTaskIds = mutableListOf<Long>()

    // Unifica los varios flows en un UiState común
    val uiState: StateFlow<AssistantState> = combine(
        chatAIService.messages,
        _tasks,
        chatAIService.isLoading,
        chatAIService.error,
        _viewModelInfo
    ) { messages, tasks, isLoading, error, viewModelInfo ->
        AssistantState(
            messages = messages,
            tasks = tasks,
            isLoading = isLoading,
            chatAIServiceError = error,
            viewModelInfo = viewModelInfo
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AssistantState()
    )

    // Estado derivado correspondiente al parsing del último mensaje
    private val _lastResponse = MutableStateFlow<AssistantResponse?>(null)
    val lastResponse: StateFlow<AssistantResponse?> = _lastResponse.asStateFlow()

    init {
        // Se crea la corrutina que recibe los mensajes del asistente
        viewModelScope.launch {
            // Escuchar permanentemente el flujo de mensajes
            chatAIService.messages.collectLatest { messages ->

                // Obtener el último mensaje de la conversación (si existe)
                val lastMessage = messages.lastOrNull()

                if (lastMessage != null) {
                    // Intentar extraer un bloque JSON del contenido del último mensaje
                    val response = responseParser.parse(lastMessage.content)

                    Logger.d("IBC-AssistantViewModel", "response = $response")

                    if (response.hasJson) {
                        // Si se ha encontrado JSON válido → extraer tareas y asignarlas
                        _tasks.value = taskJsonExtractor.extractTasksFromText(response.json)
                        // Guardar el AssistantResponse para CommentsCard o similares
                        _lastResponse.value = response
                    } else {
                        // Si no se encontró JSON → anular lastResponse --> NO: se pierde el comentario
                        // _lastResponse.value = null
                        _lastResponse.value = response
                    }
                } else {
                    // Si no hay ningún mensaje → anular lastResponse
                    _lastResponse.value = null
                }
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            chatAIService.initSession()
            _tasks.value = emptyList()
            _viewModelInfo.value = null
            originalTaskIds.clear()
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch { chatAIService.sendMessage(message) }
    }

    fun commitTasksFromAssistant() {
        val tasks = _tasks.value
        if (tasks.isEmpty()) return

        // ✅ IMPORTANTE: Capturar originalTaskIds ANTES del launch
        // para evitar race conditions con otras coroutines que puedan limpiarlo
        val taskIdsToDelete = originalTaskIds.toList()

        viewModelScope.launch {
            val isEditMode = taskIdsToDelete.isNotEmpty()
            if (isEditMode) {
                try {
                    taskRepository.deleteTasks(taskIdsToDelete)
                } catch (_: Exception) {}
            }

            val tasksSaved = mutableListOf<Long>()
            var saved = 0
            var failed = 0

            tasks.forEach { task ->
                try {
                    val id = taskRepository.saveTask(task)
                    tasksSaved.add(id)
                    saved++
                } catch (_: Exception) {
                    failed++
                }
            }

            _viewModelInfo.value = getCommitResultInfo(saved, 0, failed)
            Logger.d("IBC13-AssistantViewModel", "getCommitResultInfo(): _viewModelInfo.value = ${_viewModelInfo.value}")
            _commitFinished.emit(Unit)
            Logger.d("IBC13-AssistantViewModel", "commitFinished.emit: guardado finalizado")
            delay(3000)
            _viewModelInfo.value = null
            originalTaskIds.clear()
            _tasks.value = emptyList()
            //loadTasksForEditing(tasksSaved)
        }
    }




    fun clearViewModelInfo() {
        _viewModelInfo.value = null
    }

    fun loadTasksForEditing(taskIds: List<Long>) {
        viewModelScope.launch {
            val tasks = mutableListOf<Task>()
            val loadErrors = mutableListOf<String>()
            taskIds.forEach { id ->
                try {
                    taskRepository.getTask(id)?.let { tasks.add(it) }
                        ?: loadErrors.add("❌ No encontrada tarea ID $id")
                } catch (e: Exception) {
                    loadErrors.add("❌ Error tarea $id: ${e.message}")
                }
            }
            if (tasks.isNotEmpty()) {
                _tasks.value = tasks
                originalTaskIds.clear()
                originalTaskIds.addAll(taskIds)
                val userMessage = if (tasks.size == 1) "Tarea para editar"
                else "Tareas para edición múltiple"
                val json = convertListTaskToJSON(tasks)
                chatAIService.addUserAndAssistantMessage(userMessage, json)
            }

            if (loadErrors.isNotEmpty()) {
                _viewModelInfo.value = ViewModelInfo.Error(loadErrors.joinToString("\n"))
            }
        }
    }




    @Suppress("SameParameterValue")
    private fun getCommitResultInfo(saved: Int, updated: Int, failed: Int): ViewModelInfo {
        return when {
            failed == 0 && updated == 0 -> {
                val msg = if (saved == 1) "✓ Una tarea guardada"
                else "✓ $saved tareas guardadas"
                ViewModelInfo.Success(msg)
            }
            failed == 0 && saved == 0 -> {
                val msg = if (updated == 1) "Una tarea actualizada"
                else "✓ $updated tareas actualizadas"
                ViewModelInfo.Success(msg)
            }
            failed == 0 -> {
                val savedMsg = if (saved == 1) "$saved nueva" else "$saved nuevas"
                val updatedMsg = if (updated == 1) "$updated actualizada" else "$updated actualizadas"
                ViewModelInfo.Success("✓ $savedMsg + $updatedMsg")
            }
            saved + updated > 0 -> ViewModelInfo.Warning("⚠️ nuevas: $saved, actualizadas + $updated; fallidas: $failed")
            else -> ViewModelInfo.Error("❌ Ninguna tarea guardada")
        }
    }


//    private fun convertListTaskToJSON(tasks: List<Task>) =
//        Gson().toJson(mapOf("propuesta" to mapOf("tasks" to tasks)))
    private fun convertListTaskToJSON(tasks: List<Task>): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()  // Esto añade indentación y saltos de línea
            .create()

        return gson.toJson(mapOf("propuesta" to mapOf("tasks" to tasks)))
    }


    fun cancelEdit() {
        _tasks.value = emptyList()
        originalTaskIds.clear()
    }
}
