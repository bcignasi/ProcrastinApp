package com.ibc.procrastinapp.ui.navigation

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.ui.assistant.AssistantViewModel
import com.ibc.procrastinapp.utils.Logger
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AssistantSaveNavigationTest {
    private val dispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val store = ViewModelStore()
    private val proposals = MutableStateFlow(listOf(Task(title = "Propuesta")))
    private val assistant = mockk<AssistantRepository>(relaxed = true)
    private val tasks = mockk<TaskRepository>(relaxed = true)
    private val navigator = ComposeNavigator()
    private lateinit var nav: NavHostController

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { assistant.tasks } returns proposals
        every { assistant.messages } returns MutableStateFlow<List<Message>>(emptyList())
        every { assistant.isLoading } returns MutableStateFlow(false)
        every { assistant.error } returns MutableStateFlow<Throwable?>(null)
        every { assistant.lastResponse } returns MutableStateFlow<AssistantResponse?>(null)
        nav = NavHostController(RuntimeEnvironment.getApplication()).apply {
            setViewModelStore(store)
            navigatorProvider.addNavigator(navigator)
            graph = createGraph(startDestination = AGENDA_ROUTE) {
                composable(AGENDA_ROUTE) {}
                assistantDestinations { _, _ -> }
            }
        }
    }

    @After
    fun tearDown() {
        store.clear()
        applicationScope.cancel()
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun model(): AssistantViewModel {
        val factory = viewModelFactory {
            initializer { AssistantViewModel(assistant, tasks, applicationScope) }
        }
        return ViewModelProvider(nav.currentBackStackEntry!!, factory)[AssistantViewModel::class.java]
    }

    private fun finishTransitions() {
        (nav.visibleEntries.value + navigator.backStack.value).distinct().forEach {
            navigator.onTransitionComplete(it)
        }
    }

    @Test
    fun aNewGoalDoesNotCancelThePreviousProposalSave() = runTest(dispatcher) {
        proposals.value = listOf(Task(title = "Paso 1"), Task(title = "Paso 2"))
        val releaseSave = CompletableDeferred<Unit>()
        val stored = mutableListOf<Task>()
        var calls = 0
        coEvery { tasks.saveTask(any()) } coAnswers {
            if (++calls == 2) releaseSave.await()
            stored.add(firstArg())
            calls.toLong()
        }
        nav.openMainDestination(ASSISTANT_ROUTE)
        finishTransitions()
        val previous = model()
        previous.commitTasksFromAssistant()
        runCurrent()
        assertEquals(1, stored.size)

        nav.openMainDestination(AGENDA_ROUTE)
        finishTransitions()
        assertEquals(AGENDA_ROUTE, nav.currentDestination?.route)
        nav.openAssistant("chat?initSession=true")
        finishTransitions()
        assertNotSame(previous, model())
        runCurrent()
        releaseSave.complete(Unit)
        runCurrent()

        assertEquals(2, stored.size)
    }

    @Test
    fun returningAfterSaveSeesAnIdleButtonWithoutReceivingTheCompletionEvent() = runTest(dispatcher) {
        val releaseSave = CompletableDeferred<Unit>()
        var saved = 0
        coEvery { tasks.saveTask(any()) } coAnswers { releaseSave.await(); (++saved).toLong() }
        nav.openMainDestination(ASSISTANT_ROUTE)
        finishTransitions()
        val previous = model()
        previous.commitTasksFromAssistant()
        assertTrue(previous.isSaving.value)
        previous.commitTasksFromAssistant() // Un segundo toque no duplica el guardado.
        runCurrent()

        nav.openMainDestination(AGENDA_ROUTE)
        finishTransitions()
        releaseSave.complete(Unit)
        runCurrent()
        assertEquals(1, saved)
        nav.openMainDestination(ASSISTANT_ROUTE)
        finishTransitions()
        assertSame(previous, model())
        assertFalse(model().isSaving.value)

        proposals.value = listOf(Task(title = "Otra propuesta"))
        model().commitTasksFromAssistant()
        runCurrent()
        assertEquals(2, saved)
    }
}
