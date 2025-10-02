/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.utils.Logger
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: AssistantViewModel
    private lateinit var taskRepository: TaskRepository
    private lateinit var chatAIService: ChatAIService
    private lateinit var messagesFlow: MutableStateFlow<List<Message>>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mockear Logger para evitar llamadas a android.util.Log
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit

        taskRepository = mock()

        // Crear un MutableStateFlow que podamos modificar en los tests
        messagesFlow = MutableStateFlow<List<Message>>(emptyList())

        chatAIService = mock {
            on { messages } doReturn MutableStateFlow<List<Message>>(emptyList()) as StateFlow<List<Message>>
            on { isLoading } doReturn MutableStateFlow(false) as StateFlow<Boolean>
            on { error } doReturn MutableStateFlow(null) as StateFlow<String?>
        }
        viewModel = AssistantViewModel(chatAIService, taskRepository)

        // Dar tiempo para que el init {} se suscriba al flow
        testScope.advanceUntilIdle()

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should delete old tasks and insert new ones on commit`() = testScope.runTest {
        val oldTaskId = 1L
        val oldTask = Task(id = oldTaskId, title = "Vieja tarea")
        val newTask = Task(title = "Nueva propuesta")

        whenever(taskRepository.getTask(oldTaskId)).thenReturn(oldTask)
        whenever(taskRepository.saveTask(newTask)).thenReturn(100L)

        // 1️⃣ Cargar tareas en modo edición
        viewModel.loadTasksForEditing(listOf(oldTaskId))
        advanceUntilIdle()

        // 2️⃣ Verificar que originalTaskIds contiene [1]
        val idsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
        idsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val originalIds = idsField.get(viewModel) as List<Long>
        println("✅ originalTaskIds después de loadTasksForEditing = $originalIds")
        assertEquals(listOf(oldTaskId), originalIds)

        // 3️⃣ Simular que el asistente propone una nueva tarea
        val tasksField = AssistantViewModel::class.java.getDeclaredField("_tasks")
        tasksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val tasksFlow = tasksField.get(viewModel) as MutableStateFlow<List<Task>>
        tasksFlow.value = listOf(newTask)

        // 4️⃣ Verificar que originalTaskIds SIGUE teniendo [1] antes del commit
        @Suppress("UNCHECKED_CAST")
        val idsBeforeCommit = idsField.get(viewModel) as List<Long>
        println("✅ originalTaskIds ANTES de commit = $idsBeforeCommit")
        assertEquals(listOf(oldTaskId), idsBeforeCommit)

        // 5️⃣ Ejecutar commit
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        // 6️⃣ Verificaciones finales
        verify(taskRepository).deleteTasks(listOf(oldTaskId))
        verify(taskRepository).saveTask(newTask)
    }

    @Test
fun `should insert tasks without deleting if not in edit mode`() = testScope.runTest {
    val newTask = Task(title = "Tarea sin edición")
    whenever(taskRepository.saveTask(newTask)).thenReturn(200L)

    val tasksField = AssistantViewModel::class.java.getDeclaredField("_tasks")
    tasksField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = tasksField.get(viewModel) as MutableStateFlow<List<Task>>
    stateFlow.value = listOf(newTask)

    viewModel.commitTasksFromAssistant()
    advanceUntilIdle()

    verify(taskRepository).saveTask(newTask)
}

@Test
fun `should clear taskIds and tasks on cancelEdit`() = testScope.runTest {
    val oldTaskId = 42L
    whenever(taskRepository.getTask(oldTaskId)).thenReturn(Task(id = oldTaskId, title = "Tarea"))

    viewModel.loadTasksForEditing(listOf(oldTaskId))
    advanceUntilIdle()

    viewModel.cancelEdit()

    val uiTasks = viewModel.uiState.value.tasks
    assertTrue(uiTasks.isEmpty())

    val originalIdsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
    originalIdsField.isAccessible = true
    val ids = originalIdsField.get(viewModel) as List<*>
    assertTrue(ids.isEmpty())
}

}
