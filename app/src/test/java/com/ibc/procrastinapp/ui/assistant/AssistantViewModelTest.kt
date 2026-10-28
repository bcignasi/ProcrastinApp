/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.utils.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: AssistantViewModel
    private lateinit var taskRepository: TaskRepository
    private lateinit var assistantRepository: AssistantRepository

    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<Throwable?>(null)
    private val lastResponseFlow = MutableStateFlow<AssistantResponse?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit

        taskRepository = mockk(relaxed = true)
        assistantRepository = mockk(relaxed = true)

        every { assistantRepository.messages } returns messagesFlow as StateFlow<List<Message>>
        every { assistantRepository.tasks } returns tasksFlow as StateFlow<List<Task>>
        every { assistantRepository.isLoading } returns isLoadingFlow as StateFlow<Boolean>
        every { assistantRepository.error } returns errorFlow as StateFlow<Throwable?>
        every { assistantRepository.lastResponse } returns lastResponseFlow as StateFlow<AssistantResponse?>

        viewModel = AssistantViewModel(assistantRepository, taskRepository, testScope)
        testScope.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returning to a new session does not clear its proposal again`() = testScope.runTest {
        every { assistantRepository.initSession() } answers { tasksFlow.value = emptyList() }
        viewModel.enterFromNavigation(emptyList(), initSession = true)
        advanceUntilIdle()
        val proposal = Task(title = "Practicar entrevista")
        tasksFlow.value = listOf(proposal)

        viewModel.enterFromNavigation(emptyList(), initSession = true)
        advanceUntilIdle()

        assertEquals(listOf(proposal), tasksFlow.value)
    }

    @Test
    fun `unseen compares complete proposal and ignores unchanged commentary`() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val original = listOf(Task(title = "Teórico", subtasks = listOf(Task(title = "Test", scheduledDate = "2026-11-02"))))
        tasksFlow.value = original
        runCurrent()
        assertTrue(viewModel.uiState.value.hasUnseenProposal)
        viewModel.markProposalSeen(original)
        runCurrent()
        assertFalse(viewModel.uiState.value.hasUnseenProposal)
        messagesFlow.value = listOf(Message.assistantMessage("De acuerdo"))
        tasksFlow.value = original.map { it.copy() }
        runCurrent()
        assertFalse(viewModel.uiState.value.hasUnseenProposal)
        tasksFlow.value = listOf(original.single().copy(subtasks = listOf(
            original.single().subtasks.single().copy(scheduledDate = "2026-11-04")
        )))
        runCurrent()
        viewModel.markProposalSeen(original) // Efecto tardío de una pantalla anterior.
        runCurrent()
        assertTrue(viewModel.uiState.value.hasUnseenProposal)
    }

    @Test
    fun `clearing saving cancelling and restarting reset seen proposal`() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val proposal = listOf(Task(title = "Sesión"))
        every { assistantRepository.initSession() } answers { tasksFlow.value = emptyList() }
        every { assistantRepository.clearTasks() } answers { tasksFlow.value = emptyList() }
        every { assistantRepository.clearTasksIfSession(any()) } answers { tasksFlow.value = emptyList() }
        val clearActions = listOf<() -> Unit>(
            { tasksFlow.value = emptyList() },
            { viewModel.cancelEdit() },
            { viewModel.startNewSession() },
            { viewModel.commitTasksFromAssistant() }
        )
        for (clear in clearActions) {
            tasksFlow.value = proposal
            runCurrent()
            viewModel.markProposalSeen(proposal)
            runCurrent()
            assertFalse(viewModel.uiState.value.hasUnseenProposal)
            clear()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasUnseenProposal)
            tasksFlow.value = proposal
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnseenProposal)
        }
    }

    @Test
    fun `indicator waits until response finishes and empty interview has none`() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        runCurrent()
        assertFalse(viewModel.uiState.value.hasUnseenProposal)
        isLoadingFlow.value = true
        tasksFlow.value = listOf(Task(title = "Paso"))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasUnseenProposal)
        isLoadingFlow.value = false
        runCurrent()
        assertTrue(viewModel.uiState.value.hasUnseenProposal)
    }

    @Test
    fun `returning to editing does not overwrite changes with the original task`() = testScope.runTest {
        val original = Task(id = 42L, title = "Original")
        coEvery { taskRepository.getTask(42L) } returns original
        every { assistantRepository.addUserAndAssistantMessage(any(), any()) } answers {
            tasksFlow.value = listOf(original)
        }
        viewModel.enterFromNavigation(listOf(42L), initSession = false)
        advanceUntilIdle()
        val edited = original.copy(title = "Propuesta corregida")
        tasksFlow.value = listOf(edited)

        viewModel.enterFromNavigation(listOf(42L), initSession = false)
        advanceUntilIdle()

        assertEquals(listOf(edited), tasksFlow.value)
    }

    @Test
    fun `should delete old tasks and insert new ones on commit`() = testScope.runTest {
        val oldTaskId = 1L
        val oldTask = Task(id = oldTaskId, title = "Vieja tarea")
        val newTask = Task(title = "Nueva propuesta")

        coEvery { taskRepository.getTask(oldTaskId) } returns oldTask
        coEvery { taskRepository.saveTask(newTask) } returns 100L

        // 1. Cargar tareas en modo edición
        viewModel.loadTasksForEditing(listOf(oldTaskId))
        advanceUntilIdle()

        // 2. Verificar que originalTaskIds contiene [1]
        val idsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
        idsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf(oldTaskId), idsField.get(viewModel) as List<Long>)

        // 3. El repositorio tiene la nueva tarea propuesta
        tasksFlow.value = listOf(newTask)

        // 4. Ejecutar commit
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        // 5. Verificaciones
        coVerify { taskRepository.deleteTasks(listOf(oldTaskId)) }
        coVerify { taskRepository.saveTask(newTask) }
    }

    @Test
    fun `should insert tasks without deleting if not in edit mode`() = testScope.runTest {
        val newTask = Task(title = "Tarea sin edición")
        coEvery { taskRepository.saveTask(newTask) } returns 200L

        tasksFlow.value = listOf(newTask)

        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        coVerify { taskRepository.saveTask(newTask) }
        coVerify(exactly = 0) { taskRepository.deleteTasks(any()) }
    }

    @Test
    fun `should clear taskIds and tasks on cancelEdit`() = testScope.runTest {
        val oldTaskId = 42L
        coEvery { taskRepository.getTask(oldTaskId) } returns Task(id = oldTaskId, title = "Tarea")

        viewModel.loadTasksForEditing(listOf(oldTaskId))
        advanceUntilIdle()

        viewModel.cancelEdit()

        verify { assistantRepository.clearTasks() }

        val originalIdsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
        originalIdsField.isAccessible = true
        val ids = originalIdsField.get(viewModel) as List<*>
        assertTrue(ids.isEmpty())
    }
}
