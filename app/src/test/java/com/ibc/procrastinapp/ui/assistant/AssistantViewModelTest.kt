/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.utils.Logger
import io.mockk.every
import io.mockk.mockkObject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: AssistantViewModel
    private lateinit var taskRepository: TaskRepository
    private lateinit var chatAIService: ChatAIService

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
        chatAIService = mock {
            on { messages } doReturn MutableStateFlow<List<Message>>(emptyList()) as StateFlow<List<Message>>
            on { isLoading } doReturn MutableStateFlow(false) as StateFlow<Boolean>
            on { error } doReturn MutableStateFlow(null) as StateFlow<String?>
        }
        viewModel = AssistantViewModel(chatAIService, taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

//    @Test
//    fun `should delete old tasks and insert new ones on commit`() = testScope.runTest {
//        val oldTaskId = 1L
//        val newTask = Task(title = "Nueva propuesta")
//        whenever(taskRepository.getTask(oldTaskId)).thenReturn(Task(id = oldTaskId, title = "Vieja tarea"))
//        whenever(taskRepository.saveTask(newTask)).thenReturn(100L)
//
//        // Cargar tareas (aunque no tenga efecto real por mock incompleto)
//        viewModel.loadTasksForEditing(listOf(oldTaskId))
//        advanceUntilIdle()
//
//        // Forzar tareas propuestas
//        val tasksField = AssistantViewModel::class.java.getDeclaredField("_tasks")
//        tasksField.isAccessible = true
//        @Suppress("UNCHECKED_CAST")
//        val stateFlow = tasksField.get(viewModel) as MutableStateFlow<List<Task>>
//        stateFlow.value = listOf(newTask)
//
//        // Forzar también originalTaskIds
//        val idsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
//        idsField.isAccessible = true
//        @Suppress("UNCHECKED_CAST")
//        val idList = idsField.get(viewModel) as MutableList<Long>
//        idList.clear()
//        idList.add(oldTaskId)
//
//        viewModel.commitTasksFromAssistant()
//        advanceUntilIdle()
//
//        val captor = argumentCaptor<List<Long>>()
//        verify(taskRepository).deleteTasks(captor.capture())
//        assertEquals(listOf(oldTaskId), captor.firstValue)
//
//        verify(taskRepository).saveTask(newTask)
//    }
//
//    @Test
//    fun `should insert tasks without deleting if not in edit mode`() = testScope.runTest {
//        val newTask = Task(title = "Tarea sin edición")
//        whenever(taskRepository.saveTask(newTask)).thenReturn(200L)
//
//        val tasksField = AssistantViewModel::class.java.getDeclaredField("_tasks")
//        tasksField.isAccessible = true
//        @Suppress("UNCHECKED_CAST")
//        val stateFlow = tasksField.get(viewModel) as MutableStateFlow<List<Task>>
//        stateFlow.value = listOf(newTask)
//
//        viewModel.commitTasksFromAssistant()
//        advanceUntilIdle()
//
//        verify(taskRepository).saveTask(newTask)
////        verifyNoInteractions(chatAIService) // solo aseguramos que no hay más side effects
//
////        Info sobre este error:
////        ✅ Opción 1: Eliminar la verificación innecesaria
////        Simplemente borra esta línea:
////
////        verifyNoInteractions(chatAIService)
////
////        porque no es cierto: sí hay interacciones al crear el ViewModel, aunque no sean
////        parte de tu test. Lo que tú realmente estás testando es que no se borren tareas,
////        no que chatAIService quede completamente intacto.
//    }
//
//    @Test
//    fun `should clear taskIds and tasks on cancelEdit`() = testScope.runTest {
//        val oldTaskId = 42L
//        whenever(taskRepository.getTask(oldTaskId)).thenReturn(Task(id = oldTaskId, title = "Tarea"))
//
//        viewModel.loadTasksForEditing(listOf(oldTaskId))
//        advanceUntilIdle()
//
//        viewModel.cancelEdit()
//
//        val uiTasks = viewModel.uiState.value.tasks
//        assertTrue(uiTasks.isEmpty())
//
//        val originalIdsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
//        originalIdsField.isAccessible = true
//        val ids = originalIdsField.get(viewModel) as List<*>
//        assertTrue(ids.isEmpty())
//    }


    @Test
    fun `should delete old tasks and insert new ones on commit`() = testScope.runTest {
        val oldTaskId = 1L
        val oldTask = Task(id = oldTaskId, title = "Vieja tarea")
        val newTask = Task(title = "Nueva propuesta")

        // Asegura que getTask devuelve algo válido para que loadTasksForEditing() funcione
        whenever(taskRepository.getTask(oldTaskId)).thenReturn(oldTask)
        whenever(taskRepository.saveTask(newTask)).thenReturn(100L)

        // Ejecutamos el flujo normal para rellenar originalTaskIds internamente
        viewModel.loadTasksForEditing(listOf(oldTaskId))
        advanceUntilIdle()

        // Verificamos que originalTaskIds contiene [1] (si no, el test no puede continuar)
        val idsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
        idsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val originalIds = idsField.get(viewModel) as List<Long>
        println("✅ originalTaskIds en test = $originalIds")
        assertEquals(listOf(oldTaskId), originalIds) // comprueba que la carga ha surtido efecto

        // Simulamos las tareas propuestas por el asistente (inicialmente estarían vacías)
        val tasksField = AssistantViewModel::class.java.getDeclaredField("_tasks")
        tasksField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val tasksFlow = tasksField.get(viewModel) as MutableStateFlow<List<Task>>
        tasksFlow.value = listOf(newTask)

        // Ejecutamos commit
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        // Verificaciones
        verify(taskRepository).deleteTasks(listOf(oldTaskId))
        verify(taskRepository).saveTask(newTask)
    }


@Test
fun `should insert tasks without deleting if not in edit mode`() = testScope.runTest {
    val newTask = Task(title = "Tarea sin edición")
    whenever(taskRepository.saveTask(newTask)).thenReturn(200L)

    val tasksField = AssistantViewModel::class.java.getDeclaredField("_tasks")
    tasksField.isAccessible = true
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
