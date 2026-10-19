package com.ibc.procrastinapp.data.repository

import android.app.Application
import androidx.room.Room
import com.ibc.procrastinapp.data.alarm.AlarmScheduler
import com.ibc.procrastinapp.data.local.AppDatabase
import com.ibc.procrastinapp.data.local.TaskDao
import com.ibc.procrastinapp.data.local.TaskEntity
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.tasklist.TaskListQueryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AgendaFiltersTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TaskDao
    private lateinit var repository: TaskRepository
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.taskDao()
        repository = TaskRepository(dao, database, object : AlarmScheduler {
            override fun schedule(task: Task) = Unit
            override fun cancel(idTask: Long) = Unit
        })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun todayIncludesDateOnlyAndBothEndsOfTheDayButExcludesOtherDatesAndCompleted() = runBlocking {
        val dateOnly = dao.insertTask(TaskEntity(title = "Sin hora", deadline = "$today"))
        val midnight = dao.insertTask(TaskEntity(title = "Inicio", deadline = "$today 00:00"))
        val late = dao.insertTask(TaskEntity(title = "Final", deadline = "$today 23:59"))
        dao.insertTask(TaskEntity(title = "Ayer", deadline = "${today.minusDays(1)} 23:59"))
        dao.insertTask(TaskEntity(title = "Mañana", deadline = "${today.plusDays(1)} 00:00"))
        dao.insertTask(TaskEntity(title = "Sin fecha"))
        dao.insertTask(TaskEntity(title = "Solo aviso", notify = "$today 12:00"))
        dao.insertTask(TaskEntity(title = "Hecha", deadline = "$today 12:00", completed = true))

        val tasks = repository.getTasks(TaskListQueryType.valueOf("TODAY")).first()

        assertEquals(listOf(dateOnly, midnight, late), tasks.map { it.id })
    }

    @Test
    fun todayPreservesSubtasksWithoutPromotingChildrenFromOtherParents() = runBlocking {
        val root = dao.insertTask(TaskEntity(title = "Padre de hoy", deadline = "$today"))
        val child = dao.insertTask(TaskEntity(title = "Hija", parentTaskId = root))
        val future = dao.insertTask(TaskEntity(title = "Padre futuro", deadline = "${today.plusDays(1)}"))
        dao.insertTask(TaskEntity(title = "Hija de hoy", deadline = "$today", parentTaskId = future))

        val tasks = repository.getTasks(TaskListQueryType.valueOf("TODAY")).first()

        assertEquals(listOf(root), tasks.map { it.id })
        assertEquals(listOf(child), tasks.single().subtasks.map { it.id })
    }

    @Test
    fun completingATaskRemovesItFromTodayAndPendingAndAddsItToDone() = runBlocking {
        val id = dao.insertTask(TaskEntity(title = "Por hacer", deadline = "$today"))
        val todayQuery = repository.getTasks(TaskListQueryType.valueOf("TODAY"))
        assertEquals(listOf(id), todayQuery.first().map { it.id })

        repository.setTaskCompleted(id, true)

        assertTrue(todayQuery.first().isEmpty())
        assertTrue(repository.getTasks(TaskListQueryType.ALL).first().isEmpty())
        assertEquals(listOf(id), repository.getTasks(TaskListQueryType.COMPLETED).first().map { it.id })
    }

    @Test
    fun originalNowStillIncludesTomorrowAndPendingStillIncludesUndatedTasks() = runBlocking {
        val tomorrow = dao.insertTask(TaskEntity(title = "Mañana", deadline = "${today.plusDays(1)}"))
        val undated = dao.insertTask(TaskEntity(title = "Sin fecha"))

        assertEquals(listOf(tomorrow), repository.getTasks(TaskListQueryType.NOW).first().map { it.id })
        assertEquals(setOf(tomorrow, undated), repository.getTasks(TaskListQueryType.ALL).first().map { it.id }.toSet())
    }
}
