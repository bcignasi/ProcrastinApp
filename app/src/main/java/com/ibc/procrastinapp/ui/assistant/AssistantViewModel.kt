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
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.repository.ProjectDestination
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.ui.assistant.AssistantState.AssistantEvent
import com.ibc.procrastinapp.ui.assistant.AssistantState.LoadTaskError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
    private val taskRepository: TaskRepository,
    private val saveScope: CoroutineScope
) : ViewModel() {

    // Evento tipado que la UI traduce a string localizada
    private val _event = MutableStateFlow<AssistantEvent?>(null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Solo se emite tras guardar correctamente y sin avisos pendientes de revisar.
    private val _commitFinished = MutableSharedFlow<Unit>()
    val commitFinished = _commitFinished.asSharedFlow()

    private val projectDestination = MutableStateFlow<ProjectDestination>(ProjectDestination.Keep)

    fun projects() = taskRepository.projects()

    fun chooseProject(destination: ProjectDestination) {
        if (!_isSaving.value) projectDestination.value = destination
    }

    private val originalGoals = MutableStateFlow<List<Task>>(emptyList())
    private var editLoadJob: Job? = null
    private var editLoadErrors: List<LoadTaskError> = emptyList()

    // Estado derivado correspondiente al parsing del último mensaje
    val lastResponse: StateFlow<AssistantResponse?> = assistantRepository.lastResponse

    // Solo estado de presentación; no modifica las tareas ni el historial.
    private val viewedProposal = MutableStateFlow<List<Task>>(emptyList())
    private val proposalState = combine(
        assistantRepository.tasks.onEach { tasks ->
            if (tasks.isEmpty()) viewedProposal.value = emptyList()
        },
        viewedProposal
    ) { tasks, viewed -> tasks to (tasks.isNotEmpty() && tasks != viewed) }

    fun markProposalSeen(proposal: List<Task>) {
        // Un efecto de una composición anterior no debe reconocer una propuesta nueva.
        if (proposal.isNotEmpty() && proposal == assistantRepository.tasks.value) {
            viewedProposal.value = proposal.toList()
        }
    }

    // Unifica los varios flows en un UiState común
    val uiState: StateFlow<AssistantState> = combine(
        assistantRepository.messages,
        proposalState,
        assistantRepository.isLoading,
        assistantRepository.error,
        _event
    ) { messages, proposal, isLoading, error, event ->
        AssistantState(
            messages = messages,
            tasks = proposal.first,
            hasUnseenProposal = proposal.second && !isLoading,
            isLoading = isLoading,
            chatAIServiceError = error,
            event = event
        )
    }.combine(projectDestination) { state, destination ->
        state.copy(projectDestination = destination)
    }.combine(originalGoals) { state, goals ->
        state.copy(editingUnassignedGoals = goals.isNotEmpty() && goals.all { it.projectId == null })
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AssistantState()
    )

    private var navigationInitialized = false

    // A restored tab keeps its ViewModel: do not replay its initial request.
    fun enterFromNavigation(taskIds: List<Long>, initSession: Boolean) {
        if (navigationInitialized) return
        navigationInitialized = true
        if (initSession) startNewSession()
        else if (taskIds.isNotEmpty()) loadTasksForEditing(taskIds)
    }

    fun startNewSession() {
        if (_isSaving.value) return
        editLoadJob?.cancel()
        editLoadErrors = emptyList()
        viewModelScope.launch {
            viewedProposal.value = emptyList()
            projectDestination.value = ProjectDestination.Keep
            assistantRepository.initSession()
            originalGoals.value = emptyList()
            _event.value = null
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || _isSaving.value) return
        viewModelScope.launch { assistantRepository.sendMessage(message) }
    }

    fun commitTasksFromAssistant() {
        if (editLoadJob?.isActive == true) return
        if (editLoadErrors.isNotEmpty()) {
            _event.value = AssistantEvent.LoadFailed(editLoadErrors)
            return
        }
        val tasks = assistantRepository.tasks.value
        if (tasks.isEmpty() || !_isSaving.compareAndSet(false, true)) return

        val originalIds = originalGoals.value.map { it.id }
        val destination = projectDestination.value
        val sessionVersion = assistantRepository.sessionVersion

        // La operación vive en la aplicación, aunque se descarte esta entrada de navegación.
        saveScope.launch(Dispatchers.Main.immediate) {
            _event.value = null
            try {
                val result = taskRepository.saveProposal(originalIds, tasks, destination)
                projectDestination.value = ProjectDestination.Keep
                originalGoals.value = emptyList()
                assistantRepository.clearTasksIfSession(sessionVersion)
                if (assistantRepository.tasks.value.isEmpty()) viewedProposal.value = emptyList()
                if (result.reminderFailures > 0) {
                    _event.value = AssistantEvent.CommitRemindersFailed
                } else {
                    _event.value = AssistantEvent.CommitDone(saved = result.rootIds.size, updated = 0, failed = 0)
                    _commitFinished.emit(Unit)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Mantener tanto el borrador como los IDs originales permite corregir y reintentar.
                _event.value = AssistantEvent.CommitFailed
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearEvent() {
        _event.value = null
    }

    fun loadTasksForEditing(taskIds: List<Long>): Job? {
        if (_isSaving.value || taskIds.isEmpty()) return null
        editLoadJob?.cancel()
        editLoadJob = viewModelScope.launch {
            val tasks = mutableListOf<Task>()
            val loadErrors = mutableListOf<LoadTaskError>()
            taskIds.distinct().forEach { id ->
                try {
                    taskRepository.getTask(id)?.let { tasks.add(it) }
                        ?: loadErrors.add(LoadTaskError.NotFound(id))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    loadErrors.add(LoadTaskError.TaskException(id, e.message))
                }
            }
            editLoadErrors = loadErrors.toList()
            if (loadErrors.isNotEmpty()) {
                // No publicar una selección parcial ni permitir guardar el borrador anterior.
                _event.value = AssistantEvent.LoadFailed(editLoadErrors)
                return@launch
            }
            if (tasks.isNotEmpty()) {
                _event.value = null
                viewedProposal.value = emptyList()
                originalGoals.value = tasks
                val projectId = tasks.map { it.projectId }.distinct().singleOrNull()
                projectDestination.value = if (projectId != null) {
                    ProjectDestination.Existing(projectId, requireNotNull(tasks.first().projectName))
                } else ProjectDestination.Keep
                val userMessage = if (tasks.size == 1) "Tarea para editar"
                else "Tareas para edición múltiple"
                val json = convertListTaskToJSON(tasks)
                assistantRepository.addUserAndAssistantMessage(userMessage, json)
            }
        }
        return editLoadJob
    }

    fun cancelEdit() {
        if (_isSaving.value) return
        editLoadJob?.cancel()
        editLoadErrors = emptyList()
        _event.value = null
        viewedProposal.value = emptyList()
        projectDestination.value = ProjectDestination.Keep
        assistantRepository.clearTasks()
        originalGoals.value = emptyList()
    }

    private fun convertListTaskToJSON(tasks: List<Task>): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        return gson.toJson(mapOf("propuesta" to mapOf("tasks" to tasks)))
    }
}
