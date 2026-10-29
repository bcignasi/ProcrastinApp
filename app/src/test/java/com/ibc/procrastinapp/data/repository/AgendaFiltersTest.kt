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
    private val today = LocalDate.of(2026, 10, 19)

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
        val dateOnly = dao.insertTask(TaskEntity(title = "Sin hora", notify = "$today"))
        val midnight = dao.insertTask(TaskEntity(title = "Inicio", notify = "$today 00:00"))
        val late = dao.insertTask(TaskEntity(title = "Final", notify = "$today 23:59"))
        dao.insertTask(TaskEntity(title = "Ayer", notify = "${today.minusDays(1)} 23:59"))
        dao.insertTask(TaskEntity(title = "Mañana", notify = "${today.plusDays(1)} 00:00"))
        dao.insertTask(TaskEntity(title = "Sin fecha"))
        dao.insertTask(TaskEntity(title = "Solo límite", deadline = "$today 12:00"))
        dao.insertTask(TaskEntity(title = "Hecha", notify = "$today 12:00", completed = true))

        val tasks = repository.getTasks(TaskListQueryType.TODAY, today).first()

        assertEquals(listOf(dateOnly, midnight, late), tasks.map { it.id })
    }

    @Test
    fun todayReturnsMatchingChildrenAsIndependentRowsWithoutUndatedDescendants() = runBlocking {
        val root = dao.insertTask(TaskEntity(title = "Padre de hoy", notify = "$today"))
        val child = dao.insertTask(TaskEntity(title = "Hija", parentTaskId = root))
        val future = dao.insertTask(TaskEntity(title = "Padre futuro", notify = "${today.plusDays(1)}"))
        val datedChild = dao.insertTask(TaskEntity(title = "Hija de hoy", scheduledDate = "$today", parentTaskId = future))

        val tasks = repository.getTasks(TaskListQueryType.TODAY, today).first()

        assertEquals(setOf(root, datedChild), tasks.map { it.id }.toSet())
        assertTrue(tasks.all { it.subtasks.isEmpty() })
        assertEquals(listOf(child), repository.getTask(root)!!.subtasks.map { it.id })
    }

    @Test
    fun scheduledDayTakesPrecedenceOverReminderAndIncludesNestedSessionsOnlyOnTheirDay() = runBlocking {
        val goal = dao.insertTask(TaskEntity(title = "Teórico"))
        val block = dao.insertTask(TaskEntity(title = "Tests", parentTaskId = goal))
        val session = dao.insertTask(TaskEntity(title = "Sesión", parentTaskId = block, scheduledDate = "$today"))
        val otherReminder = dao.insertTask(TaskEntity(title = "Hoy con aviso mañana", scheduledDate = "$today", notify = "${today.plusDays(1)} 09:00"))
        dao.insertTask(TaskEntity(title = "Mañana con aviso hoy", scheduledDate = "${today.plusDays(1)}", notify = "$today 09:00"))
        dao.insertTask(TaskEntity(title = "Atrasada", scheduledDate = "${today.minusDays(1)}", notify = "$today 09:00"))
        dao.insertTask(TaskEntity(title = "Hecha", scheduledDate = "$today", completed = true))
        assertEquals(setOf(session, otherReminder), repository.getTasks(TaskListQueryType.TODAY, today).first().map { it.id }.toSet())
        repository.setTaskCompleted(session, true)
        assertEquals(listOf(otherReminder), repository.getTasks(TaskListQueryType.TODAY, today).first().map { it.id })
        assertEquals(listOf(goal), repository.getTasks(TaskListQueryType.ALL).first().filter { it.id == goal }.map { it.id })
        assertTrue(repository.getTask(goal)!!.subtasks.single().subtasks.single().completed)
    }

    @Test
    fun bulkCompletionOfASubtaskPreservesItsParentAndDescendants() = runBlocking {
        val goal = dao.insertTask(TaskEntity(title = "Objetivo"))
        val child = dao.insertTask(TaskEntity(title = "Bloque", parentTaskId = goal, scheduledDate = "$today"))
        val grandchild = dao.insertTask(TaskEntity(title = "Sesión", parentTaskId = child))
        repository.completeTasks(listOf(child))
        assertEquals(goal, dao.getTaskByIdImmediate(child)!!.parentTaskId)
        assertEquals(child, dao.getTaskByIdImmediate(grandchild)!!.parentTaskId)
        assertTrue(repository.getTask(goal)!!.subtasks.single().completed)
        assertEquals(false, dao.getTaskByIdImmediate(grandchild)!!.completed)
    }

    @Test
    fun editingSessionsResolvesTheirWholeGoalOnceWithoutChangingStoredTasks() = runBlocking {
        val goal = dao.insertTask(TaskEntity(title = "Objetivo"))
        val child = dao.insertTask(TaskEntity(title = "Bloque", parentTaskId = goal))
        val session = dao.insertTask(TaskEntity(title = "Sesión", parentTaskId = child))
        val otherGoal = dao.insertTask(TaskEntity(title = "Otro objetivo"))
        assertEquals(listOf(goal, otherGoal), repository.getRootTaskIds(listOf(session, child, goal, otherGoal)))
        assertTrue(repository.getRootTaskIds(listOf(-1)).isEmpty())
        assertEquals(child, dao.getTaskByIdImmediate(session)!!.parentTaskId)
    }

    @Test
    fun scheduledParentAndChildAppearOnceAndNextDayExcludesBoth() = runBlocking {
        val goal = dao.insertTask(TaskEntity(title = "Objetivo", scheduledDate = "$today"))
        val child = dao.insertTask(TaskEntity(title = "Sesión", parentTaskId = goal, scheduledDate = "$today"))
        val tasks = repository.getTasks(TaskListQueryType.TODAY, today).first()
        assertEquals(listOf(goal, child), tasks.map { it.id })
        assertTrue(tasks.all { it.subtasks.isEmpty() })
        assertTrue(repository.getTasks(TaskListQueryType.TODAY, today.plusDays(1)).first().isEmpty())
    }

    @Test
    fun completingATaskRemovesItFromTodayAndPendingAndAddsItToDone() = runBlocking {
        val id = dao.insertTask(TaskEntity(title = "Por hacer", notify = "$today"))
        val todayQuery = repository.getTasks(TaskListQueryType.TODAY, today)
        assertEquals(listOf(id), todayQuery.first().map { it.id })

        repository.setTaskCompleted(id, true)

        assertTrue(todayQuery.first().isEmpty())
        assertTrue(repository.getTasks(TaskListQueryType.ALL).first().isEmpty())
        assertEquals(listOf(id), repository.getTasks(TaskListQueryType.COMPLETED).first().map { it.id })
    }

    @Test
    fun originalNowStillIncludesTomorrowAndPendingStillIncludesUndatedTasks() = runBlocking {
        val tomorrow = dao.insertTask(TaskEntity(title = "Mañana", deadline = "${LocalDate.now().plusDays(1)}"))
        val undated = dao.insertTask(TaskEntity(title = "Sin fecha"))

        assertEquals(listOf(tomorrow), repository.getTasks(TaskListQueryType.NOW).first().map { it.id })
        assertEquals(setOf(tomorrow, undated), repository.getTasks(TaskListQueryType.ALL).first().map { it.id }.toSet())
    }

    @Test
    fun movingTheReferenceDateAcrossNewYearKeepsOldRemindersInPending() = runBlocking {
        val december = LocalDate.of(2026, 12, 31)
        val january = december.plusDays(1)
        val oldTask = dao.insertTask(TaskEntity(title = "Diciembre", notify = "$december 23:59"))
        val newTask = dao.insertTask(TaskEntity(title = "Enero", notify = "$january 00:00"))

        assertEquals(listOf(oldTask), repository.getTasks(TaskListQueryType.TODAY, december).first().map { it.id })
        assertEquals(listOf(newTask), repository.getTasks(TaskListQueryType.TODAY, january).first().map { it.id })
        assertEquals(setOf(oldTask, newTask), repository.getTasks(TaskListQueryType.ALL).first().map { it.id }.toSet())
    }

    @Test
    fun todayUsesTheReminderDateWhilePendingKeepsTasksWithoutRemindersAndOlderOnes() = runBlocking {
        val todayReminder = dao.insertTask(TaskEntity(title = "Aviso hoy", notify = "$today 09:00", deadline = "${today.plusDays(1)} 12:00"))
        val tomorrowReminder = dao.insertTask(TaskEntity(title = "Aviso mañana", notify = "${today.plusDays(1)} 09:00", deadline = "$today 12:00"))
        val overdue = dao.insertTask(TaskEntity(title = "Aviso atrasado", notify = "${today.minusDays(1)} 09:00"))
        val noReminder = dao.insertTask(TaskEntity(title = "Sin aviso", deadline = "$today 10:00"))

        assertEquals(listOf(todayReminder), repository.getTasks(TaskListQueryType.TODAY, today).first().map { it.id })
        assertEquals(setOf(todayReminder, tomorrowReminder, overdue, noReminder), repository.getTasks(TaskListQueryType.ALL).first().map { it.id }.toSet())
    }
}
