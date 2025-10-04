/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ibc.procrastinapp.data.alarm.AlarmScheduler
import com.ibc.procrastinapp.data.local.AppDatabase
import com.ibc.procrastinapp.data.local.TaskDao
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.ui.tasklist.TaskListQueryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class TaskRepositoryIntegrationTest {

    private lateinit var taskDao: TaskDao
    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository
    
    // Fake AlarmScheduler para tests - no hace nada real
    private class FakeAlarmScheduler : AlarmScheduler {
        override fun schedule(task: Task) {
            // No hace nada en tests
        }
        
        override fun cancel(idTask: Long) {
            // No hace nada en tests
        }
    }

    // Se ejecuta antes de cada test: crea una base de datos en memoria y el repositorio
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // permitido solo en tests
            .build()
        taskDao = db.taskDao()
        repository = TaskRepository(taskDao, db, FakeAlarmScheduler())
    }

    // Se ejecuta después de cada test: cierra la base de datos
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Test para guardar una tarea con subtareas y recuperarla con jerarquía
    @Test
    fun saveAndRetrieveTask_withHierarchy() = runBlocking {
        val task = Task(
            title = "Test Task",
            priority = 3,
            deadline = getFutureDate(2), // la consulta FUTURE recupera a partir del segundo día futuro
            subtasks = listOf(
                Task(title = "Subtask 1"),
                Task(title = "Subtask 2")
            )
        )

        val taskId = repository.saveTask(task)
        Assert.assertTrue(taskId > 0)

        // Recuperación plana
        val flatTasks = repository.getTasks(TaskListQueryType.FUTURE).first()
        Assert.assertEquals(1, flatTasks.size)
        Assert.assertEquals("Test Task", flatTasks[0].title)

        // La jerarquía ya está incluida por defecto
        Assert.assertEquals(2, flatTasks[0].subtasks.size)
    }

    // Test para verificar que una tarea con fecha pasada va a la lista de postergadas
    @Test
    fun saveTaskWithPastDate_retrievedAsPostponed() = runBlocking {
        val task = Task(
            title = "Past Task",
            priority = 2,
            deadline = getPastDate(1)
        )

        val taskId = repository.saveTask(task)
        Assert.assertTrue(taskId > 0)

        val postponed = repository.getTasks(TaskListQueryType.PAST).first()
        Assert.assertEquals(1, postponed.size)
        Assert.assertEquals("Past Task", postponed[0].title)

        val future = repository.getTasks(TaskListQueryType.FUTURE).first()
        Assert.assertTrue(future.none { it.title == "Past Task" })
    }

    // Test para verificar que completar una tarea la mueve a la lista de completadas
    @Test
    fun completeTask_isMovedToCompletedList() = runBlocking {
        val task = Task(
            title = "Task to complete",
            priority = 1,
            deadline = getFutureDate(2)
        )
        val id = repository.saveTask(task)

        repository.setTaskCompleted(id, true)

        val completed = repository.getTasks(TaskListQueryType.COMPLETED).first()
        Assert.assertTrue(completed.any { it.id == id })

        val future = repository.getTasks(TaskListQueryType.FUTURE).first()
        Assert.assertTrue(future.none { it.id == id })
    }

    // Test para actualizar una tarea y sus subtareas
    @Test
    fun updateTask_andSubtasks_successfully() = runBlocking {
        val original = Task(
            title = "Original",
            subtasks = listOf(Task(title = "Sub1"))
        )
        val id = repository.saveTask(original)

        val loaded = repository.getTask(id)!!
        val updated = loaded.copy(
            title = "Updated",
            subtasks = listOf(
                loaded.subtasks[0].copy(title = "Updated Sub1"),
                Task(title = "New Sub2")
            )
        )
        repository.updateTask(updated)

        val reloaded = repository.getTask(id)!!
        Assert.assertEquals("Updated", reloaded.title)
        Assert.assertEquals(2, reloaded.subtasks.size)
    }

    // Test para borrar una tarea con subtareas y verificar que desaparecen todas
    @Test
    fun deleteTask_deletesSubtasksToo() = runBlocking {
        val task = Task(
            title = "Parent",
            subtasks = listOf(
                Task(title = "Child", subtasks = listOf(Task(title = "Grandchild")))
            )
        )
        val id = repository.saveTask(task)

        val before = repository.getTask(id)
        Assert.assertNotNull(before)
        Assert.assertEquals(1, before!!.subtasks.size)

        repository.deleteTask(id)

        val after = repository.getTask(id)
        Assert.assertNull(after)
    }

    // Test para completar varias tareas de una vez
    @Test
    fun completeMultipleTasks_marksAllAsCompleted() = runBlocking {
        val tasks = listOf(
            Task(title = "Task 1", deadline = getFutureDate(1)),
            Task(title = "Task 2", deadline = getFutureDate(2)),
            Task(title = "Task 3", deadline = getFutureDate(3))
        )

        val ids = tasks.map { repository.saveTask(it) }

        repository.completeTasks(ids)

        val completedTasks = repository.getTasks(TaskListQueryType.COMPLETED).first()
        val completedTitles = completedTasks.map { it.title }

        Assert.assertEquals(3, completedTitles.size)
        Assert.assertTrue(completedTitles.containsAll(listOf("Task 1", "Task 2", "Task 3")))
    }

    // Test para borrar varias tareas de una vez
    @Test
    fun deleteMultipleTasks_removesAllOfThem() = runBlocking {
        val tasks = listOf(
            Task(title = "Task A", deadline = getFutureDate(1)),
            Task(title = "Task B", deadline = getFutureDate(2)),
            Task(title = "Task C", deadline = getFutureDate(3))
        )

        val ids = tasks.map { repository.saveTask(it) }

        val allBefore = repository.getTasks(TaskListQueryType.ALL).first()
        Assert.assertTrue(allBefore.any { it.title == "Task A" })

        repository.deleteTasks(ids)

        val allAfter = repository.getTasks(TaskListQueryType.ALL).first()
        Assert.assertTrue(allAfter.none { it.title in listOf("Task A", "Task B", "Task C") })
    }

    // ==== Helpers para generar fechas ====

    private fun getFutureDate(days: Int): String {
        return LocalDateTime.now().plusDays(days.toLong())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    @Suppress("SameParameterValue")
    private fun getPastDate(days: Int): String {
        return LocalDateTime.now().minusDays(days.toLong())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}
