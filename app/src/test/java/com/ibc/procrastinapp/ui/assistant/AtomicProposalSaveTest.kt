package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.alarm.AlarmScheduler
import com.ibc.procrastinapp.data.local.AppDatabase
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.data.service.TaskJsonExtractor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/** Room real; solo se simulan el chat y las alarmas externas. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AtomicProposalSaveTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var model: AssistantViewModel
    private lateinit var scope: CoroutineScope
    private val store = ViewModelStore()
    private val draft = MutableStateFlow<List<Task>>(emptyList())
    private val assistant = mockk<AssistantRepository>(relaxed = true)
    private val alarmCalls = CopyOnWriteArrayList<String>()
    private val completions = CopyOnWriteArrayList<Unit>()
    private var failAlarms = false
    private var checkAlarmState: (() -> Unit)? = null

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = TaskRepository(db.taskDao(), db, object : AlarmScheduler {
            override fun schedule(task: Task) {
                checkAlarmState?.invoke()
                alarmCalls.add("schedule:${task.id}")
                if (failAlarms) throw SecurityException("Alarm permission denied")
            }
            override fun cancel(idTask: Long) {
                checkAlarmState?.invoke()
                alarmCalls.add("cancel:$idTask")
            }
        })
        every { assistant.tasks } returns draft
        every { assistant.messages } returns MutableStateFlow<List<Message>>(emptyList())
        every { assistant.isLoading } returns MutableStateFlow(false)
        every { assistant.error } returns MutableStateFlow<Throwable?>(null)
        every { assistant.lastResponse } returns MutableStateFlow<AssistantResponse?>(null)
        every { assistant.sessionVersion } returns 1L
        every { assistant.addUserAndAssistantMessage(any(), any()) } answers {
            draft.value = TaskJsonExtractor().extractTasks(secondArg())
        }
        every { assistant.clearTasksIfSession(1L) } answers { draft.value = emptyList() }
        model = AssistantViewModel(assistant, repository, scope)
        store.put("assistant", model)
        scope.launch { model.uiState.collect {} }
        scope.launch { model.commitFinished.collect { completions.add(it) } }
    }

    @After fun cleanup() {
        store.clear()
        scope.cancel()
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun edit(vararg ids: Long) {
        model.loadTasksForEditing(ids.toList())
        withTimeout(5_000) { draft.first { it.isNotEmpty() } }
    }

    private suspend fun save() {
        model.commitTasksFromAssistant()
        withTimeout(5_000) { model.isSaving.first { !it } }
    }

    @Test fun failedRootReplacementKeepsOriginalsDraftAndSupportsRetry() = runBlocking {
        val first = repository.saveTask(Task(title = "Original A", subtasks = listOf(Task(title = "Paso original"))))
        val second = repository.saveTask(Task(title = "Original B"))
        val blocker = repository.saveTask(Task(title = "Título ocupado"))
        val originals = listOf(repository.getTask(first), repository.getTask(second))
        edit(first, second)
        val proposal = listOf(Task(title = "Reemplazo válido"), Task(title = "Título ocupado"))
        draft.value = proposal
        alarmCalls.clear()
        save()
        assertEquals(originals, listOf(repository.getTask(first), repository.getTask(second)))
        assertEquals(proposal, draft.value)
        assertEquals(AssistantState.AssistantEvent.CommitFailed, model.uiState.value.event)
        assertTrue(completions.isEmpty())
        assertTrue(alarmCalls.isEmpty())
        assertEquals(setOf(first, second, blocker), db.taskDao().getPendingTasks().first().map { it.id }.toSet())

        draft.value = listOf(Task(title = "Reemplazo A"), Task(title = "Reemplazo B"))
        save()
        withTimeout(5_000) { while (completions.isEmpty()) yield() }
        assertNull(repository.getTask(first))
        assertNull(repository.getTask(second))
        assertTrue(draft.value.isEmpty())
        assertEquals(setOf("Reemplazo A", "Reemplazo B", "Título ocupado"), db.taskDao().getPendingTasks().first().map { it.title }.toSet())
        assertEquals(1, completions.size)
    }

    @Test fun nestedCollisionRollsBackAllWritesAndDoesNotTouchAlarms() = runBlocking {
        val root = repository.saveTask(Task(title = "Original", subtasks = listOf(Task(title = "Hijo original", completed = true))))
        val old = repository.getTask(root)
        repository.saveTask(Task(title = "Colisión"))
        edit(root)
        val proposal = listOf(Task(title = "Nueva raíz", subtasks = listOf(
            Task(title = "Paso válido", scheduledDate = "2030-02-03"), Task(title = "Colisión")
        )))
        draft.value = proposal
        alarmCalls.clear()
        save()
        assertEquals(old, repository.getTask(root))
        assertEquals(proposal, draft.value)
        assertTrue(alarmCalls.isEmpty())
        assertTrue(completions.isEmpty())
        assertEquals(setOf("Original", "Colisión"), db.taskDao().getPendingTasks().first().map { it.title }.toSet())
    }

    @Test fun newProposalIsAlsoAllOrNothing() = runBlocking {
        val proposal = listOf(Task(title = "Duplicada"), Task(title = "Duplicada"))
        draft.value = proposal
        save()
        assertTrue(db.taskDao().getPendingTasks().first().isEmpty())
        assertEquals(proposal, draft.value)
        assertTrue(alarmCalls.isEmpty())
        assertTrue(completions.isEmpty())
    }

    @Test fun successCommitsHierarchyBeforeUpdatingAnyReminder() = runBlocking {
        val root = repository.saveTask(Task(title = "Original", subtasks = listOf(Task(title = "Aviso antiguo"))))
        val oldChild = repository.getTask(root)!!.subtasks.single().id
        edit(root)
        draft.value = listOf(Task(id = root, title = "Actualizada", subtasks = listOf(
            Task(title = "Sesión", scheduledDate = "2030-02-03", notify = "2030-02-03 18:00")
        )))
        alarmCalls.clear()
        checkAlarmState = {
            assertFalse(db.inTransaction())
            db.openHelper.readableDatabase.query("SELECT title FROM tasks WHERE id = $root").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Actualizada", cursor.getString(0))
            }
        }
        save()
        withTimeout(5_000) { while (completions.isEmpty()) yield() }
        val child = repository.getTask(root)!!.subtasks.single()
        assertEquals("2030-02-03", child.scheduledDate)
        assertEquals(root, db.taskDao().getTaskByIdImmediate(child.id)!!.parentTaskId)
        assertEquals(listOf("cancel:$root", "cancel:$oldChild", "schedule:${child.id}"), alarmCalls.toList())
        assertTrue(draft.value.isEmpty())
    }

    @Test fun reminderFailureKeepsCommittedDataAndReportsWarningInsteadOfInvitingDuplicateSave() = runBlocking {
        draft.value = listOf(Task(title = "Plan con aviso", notify = "2030-02-03 18:00"))
        failAlarms = true
        save()
        assertEquals(listOf("Plan con aviso"), db.taskDao().getPendingTasks().first().map { it.title })
        assertTrue(draft.value.isEmpty())
        assertEquals(AssistantState.AssistantEvent.CommitRemindersFailed, model.uiState.value.event)
        assertTrue(completions.isEmpty())
        assertFalse(model.isSaving.value)
        model.commitTasksFromAssistant()
        assertEquals(1, alarmCalls.size)
    }
}
