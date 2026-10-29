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
import com.ibc.procrastinapp.data.repository.ProposalSaveResult
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
        coEvery { taskRepository.saveProposal(any(), any()) } returns ProposalSaveResult(listOf(100L))
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
    fun `should pass originals and proposal to the atomic operation`() = testScope.runTest {
        val oldTaskId = 1L
        val oldTask = Task(id = oldTaskId, title = "Vieja tarea")
        val newTask = Task(title = "Nueva propuesta")

        coEvery { taskRepository.getTask(oldTaskId) } returns oldTask

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
        coVerify { taskRepository.saveProposal(listOf(oldTaskId), listOf(newTask)) }
        coVerify(exactly = 0) { taskRepository.deleteTasks(any()) }
    }

    @Test
    fun `should insert tasks without deleting if not in edit mode`() = testScope.runTest {
        val newTask = Task(title = "Tarea sin edición")

        tasksFlow.value = listOf(newTask)

        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        coVerify { taskRepository.saveProposal(emptyList(), listOf(newTask)) }
        coVerify(exactly = 0) { taskRepository.deleteTasks(any()) }
    }

    @Test
    fun `partial edit load keeps both originals and can retry the complete selection`() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val originals = mutableMapOf(1L to Task(id = 1, title = "A"), 2L to Task(id = 2, title = "B"))
        coEvery { taskRepository.getTask(1L) } answers { originals[1L] }
        coEvery { taskRepository.getTask(2L) } throws IllegalStateException("Temporary read failure")
        every { assistantRepository.addUserAndAssistantMessage(any(), any()) } answers {
            tasksFlow.value = com.ibc.procrastinapp.data.service.TaskJsonExtractor().extractTasks(secondArg())
        }
        coEvery { taskRepository.saveProposal(any(), any()) } answers {
            firstArg<List<Long>>().forEach { originals.remove(it) }
            ProposalSaveResult(listOf(10L))
        }

        viewModel.loadTasksForEditing(listOf(1L, 2L))
        advanceUntilIdle()
        viewModel.clearEvent() // Dismissing an error must not enable a partial replacement.
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()
        assertEquals(setOf(1L, 2L), originals.keys)
        assertTrue(tasksFlow.value.isEmpty())
        assertTrue(viewModel.uiState.value.event is AssistantState.AssistantEvent.LoadFailed)

        coEvery { taskRepository.getTask(2L) } answers { originals[2L] }
        viewModel.loadTasksForEditing(listOf(1L, 2L))
        advanceUntilIdle()
        // El parser asigna IDs nuevos a la propuesta; los originales se conservan aparte.
        assertEquals(listOf("A", "B"), tasksFlow.value.map { it.title })
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()
        coVerify(exactly = 1) { taskRepository.saveProposal(listOf(1L, 2L), any()) }
    }

    @Test
    fun `missing edit task blocks saving an existing draft until editing is cancelled`() = testScope.runTest {
        val previous = listOf(Task(title = "Previous draft"))
        tasksFlow.value = previous
        coEvery { taskRepository.getTask(1L) } returns Task(id = 1, title = "A")
        coEvery { taskRepository.getTask(2L) } returns null
        viewModel.loadTasksForEditing(listOf(1L, 2L))
        advanceUntilIdle()
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()
        assertEquals(previous, tasksFlow.value)
        coVerify(exactly = 0) { taskRepository.saveProposal(any(), any()) }

        viewModel.cancelEdit()
        tasksFlow.value = listOf(Task(title = "New goal"))
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()
        coVerify(exactly = 1) { taskRepository.saveProposal(emptyList(), tasksFlow.value) }
    }

    @Test
    fun `cancel during edit loading does not publish a late proposal`() = testScope.runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        coEvery { taskRepository.getTask(1L) } coAnswers {
            gate.await()
            Task(id = 1, title = "Late task")
        }
        viewModel.loadTasksForEditing(listOf(1L))
        runCurrent()
        viewModel.cancelEdit()
        gate.complete(Unit)
        advanceUntilIdle()
        verify(exactly = 0) { assistantRepository.addUserAndAssistantMessage(any(), any()) }
    }

    @Test
    fun `saving waits for the entire selection and new session cancels the load`() = testScope.runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        tasksFlow.value = listOf(Task(title = "Old draft"))
        coEvery { taskRepository.getTask(1L) } coAnswers {
            gate.await()
            Task(id = 1, title = "Original")
        }
        every { assistantRepository.initSession() } answers { tasksFlow.value = emptyList() }
        viewModel.loadTasksForEditing(listOf(1L))
        runCurrent()
        viewModel.commitTasksFromAssistant()
        runCurrent()
        coVerify(exactly = 0) { taskRepository.saveProposal(any(), any()) }

        viewModel.startNewSession()
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        verify(exactly = 0) { assistantRepository.addUserAndAssistantMessage(any(), any()) }
        tasksFlow.value = listOf(Task(title = "New session proposal"))
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()
        coVerify(exactly = 1) { taskRepository.saveProposal(emptyList(), tasksFlow.value) }
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
