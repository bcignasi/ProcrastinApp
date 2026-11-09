package com.ibc.procrastinapp.data.local

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ibc.procrastinapp.data.alarm.AlarmSchedulerImpl
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.repository.ProjectDestination
import com.ibc.procrastinapp.ui.tasklist.TaskListQueryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.app.AlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class ScheduledDatePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "scheduled-date-migration-test"
    private var database: AppDatabase? = null

    @After
    fun close() {
        database?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun migrationPreservesOldRowsHierarchyAndAllowsDatedSessionsAfterReopening() = runBlocking {
        createVersionOne()
        var db = open()
        val original = db.taskDao().getTaskByIdImmediate(7)!!
        assertEquals(TaskEntity(id = 7, title = "Meta antigua", deadline = "2026-12-01 18:00",
            priority = 2, notes = "Notas originales", notify = "2026-11-01 09:00"), original)
        val child = db.taskDao().getSubtasksImmediate(7).single()
        assertEquals(TaskEntity(id = 8, title = "Paso antiguo", completed = true,
            periodicity = "Semanal", parentTaskId = 7), child)

        db.taskDao().updateTask(child.copy(scheduledDate = "2026-11-02"))
        db.close()
        db = open()
        assertEquals("2026-11-02", db.taskDao().getTaskByIdImmediate(8)!!.scheduledDate)
        assertEquals(7L, db.taskDao().getTaskByIdImmediate(8)!!.parentTaskId)

        // Mismo título, distintos días: sesiones válidas, no un conflicto de unicidad.
        val first = db.taskDao().insertTask(TaskEntity(title = "Hacer un test", scheduledDate = "2026-11-02"))
        val second = db.taskDao().insertTask(TaskEntity(title = "Hacer un test", scheduledDate = "2026-11-04"))
        assertNotEquals(first, second)
    }

    @Test
    fun repositoryPreservesNestedDaysWithoutSchedulingAlarms() = runBlocking {
        var db = open()
        val scheduler = AlarmSchedulerImpl(context)
        val repository = TaskRepository(db.taskDao(), db, scheduler)
        val rootId = repository.saveTask(Task(title = "Teórico", subtasks = listOf(
            Task(title = "Práctica", subtasks = listOf(
                Task(title = "Hacer un test", scheduledDate = "2026-11-02"),
                Task(title = "Hacer un test", scheduledDate = "2026-11-04")
            ))
        )))
        db.close()
        db = open()
        val restored = TaskRepository(db.taskDao(), db, scheduler).getTask(rootId)!!
        val sessions = restored.subtasks.single().subtasks
        assertEquals(listOf("2026-11-02", "2026-11-04"), sessions.map { it.scheduledDate }.sorted())
        assertTrue(sessions.all { it.notify.isEmpty() && it.deadline.isEmpty() })
        assertTrue(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
    }

    @Test
    fun projectAndSessionProgressSurviveReopeningAndEditing() = runBlocking {
        val scheduler = AlarmSchedulerImpl(context)
        var db = open()
        var repository = TaskRepository(db.taskDao(), db, scheduler)
        val interviews = Task(title = "Entrevistas", subtasks = listOf(
            Task(title = "Simulacro"), Task(title = "Diseñar una API")
        ))
        val id = repository.saveProposal(emptyList(), listOf(interviews),
            ProjectDestination.Create(" Empleo ")).rootIds.single()
        val project = repository.projects().first().single()
        assertEquals("Empleo", project.name)
        val destination = ProjectDestination.Existing(project.id, project.name)
        val portfolio = repository.saveProposal(emptyList(), listOf(Task(title = "Portfolio")), destination).rootIds.single()
        repository.saveProposal(emptyList(), listOf(interviews), ProjectDestination.Create("Formación"))
        db.close()

        db = open()
        repository = TaskRepository(db.taskDao(), db, scheduler)
        val saved = repository.getTask(id)!!
        val first = saved.subtasks.single { it.title == "Simulacro" }
        val second = saved.subtasks.single { it.title == "Diseñar una API" }
        // La Agenda puede marcar una sesión después de que el asistente lea su borrador.
        repository.setTaskCompleted(first.id, true)
        val edit = saved.copy(subtasks = listOf(first, second.copy(scheduledDate = "2030-02-03")))
        assertEquals(listOf(id), repository.saveProposal(listOf(id), listOf(edit)).rootIds)
        val grouped = repository.getTasks(TaskListQueryType.GOALS).first().filter { it.projectId == project.id }
        assertEquals(setOf(id, portfolio), grouped.map { it.id }.toSet())
        val result = grouped.single { it.id == id }
        assertEquals("Empleo", result.projectName)
        assertEquals(first.copy(completed = true), result.subtasks.single { it.id == first.id })
        assertEquals(second.copy(scheduledDate = "2030-02-03"), result.subtasks.single { it.id == second.id })
    }

    @Test
    fun classicEditOfPhaseKeepsItsProjectHierarchyAndSessionProgress() = runBlocking {
        val db = open()
        val repository = TaskRepository(db.taskDao(), db, AlarmSchedulerImpl(context))
        val goal = Task(title = "Entrevistas", subtasks = listOf(Task(title = "Diseño", subtasks = listOf(
            Task(title = "Simulacro")
        ))))
        val rootId = repository.saveProposal(emptyList(), listOf(goal), ProjectDestination.Create("Empleo")).rootIds.single()
        val original = repository.getTask(rootId)!!
        val phase = original.subtasks.single()
        val session = phase.subtasks.single()
        repository.setTaskCompleted(session.id, true)
        repository.updateTaskDetails(phase.id, "Diseño de sistemas", "Preparar pizarra", phase.completed)
        val updated = repository.getTask(rootId)!!
        assertEquals(original.projectId, updated.projectId)
        assertEquals(phase.copy(title = "Diseño de sistemas", notes = "Preparar pizarra",
            subtasks = listOf(session.copy(completed = true))), updated.subtasks.single())
    }

    private fun open(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .allowMainThreadQueries()
        .build().also { database = it }

    /** Esquema v1 congelado: no usar aquí TaskEntity ni el creador de la versión actual. */
    private fun createVersionOne() {
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("""CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL, deadline TEXT NOT NULL, priority INTEGER NOT NULL,
                periodicity TEXT NOT NULL, notes TEXT NOT NULL, completed INTEGER NOT NULL,
                notify TEXT NOT NULL, parentTaskId INTEGER,
                FOREIGN KEY(parentTaskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE)
            """)
            old.execSQL("CREATE UNIQUE INDEX index_tasks_title_deadline_notify ON tasks(title, deadline, notify)")
            old.execSQL("CREATE INDEX index_tasks_parentTaskId ON tasks(parentTaskId)")
            old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table VALUES(42, 'f6cfbf91d5cdc487d43ed820c5b44180')")
            old.execSQL("INSERT INTO tasks VALUES(7, 'Meta antigua', '2026-12-01 18:00', 2, '', 'Notas originales', 0, '2026-11-01 09:00', NULL)")
            old.execSQL("INSERT INTO tasks VALUES(8, 'Paso antiguo', '', 0, 'Semanal', '', 1, '', 7)")
            old.version = 1
        }
    }
}
