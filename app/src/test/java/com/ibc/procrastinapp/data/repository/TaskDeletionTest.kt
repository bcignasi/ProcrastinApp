package com.ibc.procrastinapp.data.repository

import android.app.Application
import androidx.room.Room
import com.ibc.procrastinapp.data.alarm.AlarmScheduler
import com.ibc.procrastinapp.data.local.AppDatabase
import com.ibc.procrastinapp.data.model.Task
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class TaskDeletionTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository
    private val cancelled = mutableListOf<Long>()
    private var onCancel: (() -> Unit)? = null

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = TaskRepository(db.taskDao(), db, object : AlarmScheduler {
            override fun schedule(task: Task) = Unit
            override fun cancel(idTask: Long) {
                onCancel?.invoke()
                cancelled.add(idTask)
            }
        })
    }

    @After fun cleanup() { db.close() }

    private suspend fun seed(): Task {
        val id = repository.saveTask(Task(title = "Goal", subtasks = listOf(
            Task(title = "Child", subtasks = listOf(Task(title = "Grandchild", completed = true))),
            Task(title = "Sibling")
        )))
        return repository.getTask(id)!!
    }

    @Test fun deletingGoalCancelsEveryDescendantOnlyAfterCommit() = runBlocking {
        val goal = seed()
        val child = goal.subtasks.first { it.title == "Child" }
        val ids = setOf(goal.id, child.id, child.subtasks.single().id, goal.subtasks.first { it.title == "Sibling" }.id)
        val unrelated = repository.saveTask(Task(title = "Other goal"))
        onCancel = {
            assertFalse(db.inTransaction())
            db.openHelper.readableDatabase.query("SELECT id FROM tasks WHERE id = ${goal.id}").use {
                assertFalse(it.moveToFirst())
            }
        }
        repository.deleteTask(goal.id)
        ids.forEach { assertNull(repository.getTask(it)) }
        assertEquals(ids, cancelled.toSet())
        assertEquals(ids.size, cancelled.size)
        assertNotNull(repository.getTask(unrelated))
    }

    @Test fun overlappingSelectionCancelsEachAlarmOnceAndPreservesOtherGoals() = runBlocking {
        val goal = seed()
        val child = goal.subtasks.first { it.title == "Child" }
        val other = repository.saveTask(Task(title = "Other"))
        repository.deleteTasks(listOf(child.id, goal.id, child.id))
        assertEquals(4, cancelled.size)
        assertEquals(4, cancelled.toSet().size)
        assertNull(repository.getTask(goal.id))
        assertNotNull(repository.getTask(other))
    }

    @Test fun deletingSubtreePreservesParentAndSibling() = runBlocking {
        val goal = seed()
        val child = goal.subtasks.first { it.title == "Child" }
        repository.deleteTask(child.id)
        assertEquals(setOf(child.id, child.subtasks.single().id), cancelled.toSet())
        assertEquals(listOf("Sibling"), repository.getTask(goal.id)!!.subtasks.map { it.title })
    }

    @Test fun failedBatchDeleteRollsBackRowsAndLeavesAlarmsUntouched() = runBlocking {
        val goal = seed()
        val other = repository.saveTask(Task(title = "Protected"))
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_delete BEFORE DELETE ON tasks
            WHEN OLD.id = $other BEGIN SELECT RAISE(ABORT, 'Test deletion failure'); END
        """.trimIndent())
        val outcome = runCatching { repository.deleteTasks(listOf(goal.id, other)) }
        assertTrue(outcome.isFailure)
        assertEquals(goal, repository.getTask(goal.id))
        assertNotNull(repository.getTask(other))
        assertTrue(cancelled.isEmpty())
    }

    @Test fun cancellationFailuresDoNotSkipOtherDescendantsOrReportDatabaseFailure() = runBlocking {
        val goal = seed()
        var attempts = 0
        onCancel = {
            attempts++
            if (attempts == 1 || attempts == 3) throw IllegalStateException("Alarm service unavailable")
        }
        val outcome = runCatching { repository.deleteTask(goal.id) }
        assertNull(repository.getTask(goal.id))
        assertEquals(4, attempts)
        assertEquals(2, cancelled.size)
        assertTrue("Alarm failures must be returned as a partial result", outcome.isSuccess)
        assertEquals(2, outcome.getOrThrow().reminderFailures)
    }
}
