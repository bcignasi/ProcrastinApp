package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.rememberNavController
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.quotes.QuoteAIService
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "en-w320dp-h900dp")
class ProposalDiscoveryTest {
    @get:Rule val compose = createComposeRule()
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val messages = MutableStateFlow(listOf(Message.userMessage("My goal")))
    private val loading = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private val unread = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

    @After fun close() { store.clear(); scope.cancel(); unmockkAll() }

    private fun show(height: Int = 900) {
        val repository = mockk<AssistantRepository>(relaxed = true)
        every { repository.messages } returns messages
        every { repository.tasks } returns tasks
        every { repository.isLoading } returns loading
        every { repository.error } returns MutableStateFlow(null)
        every { repository.lastResponse } returns MutableStateFlow(null)
        val model = AssistantViewModel(repository, mockk<TaskRepository>(relaxed = true), scope)
        val quotes = QuoteViewModel(mockk<QuoteAIService>(relaxed = true))
        store.put("assistant", model)
        store.put("quotes", quotes)
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.height(height.dp)) {
                    AssistantScreen(navController = rememberNavController(), assistantViewModel = model,
                        quoteViewModel = quotes)
                }
            }
        }
    }

    @Test fun newPlanIsDiscoverableAndChangesAreMarkedOnlyUntilViewed() {
        show()
        compose.onNodeWithText("View proposal").assertDoesNotExist()
        compose.onAllNodes(unread).assertCountEquals(0)
        compose.runOnIdle {
            messages.value = messages.value + List(15) { Message.assistantMessage("Message $it") }
            tasks.value = listOf(Task(title = "Study", scheduledDate = "2026-11-02"))
        }
        compose.onNodeWithText("View proposal").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            "New proposal not yet viewed")).assertIsDisplayed()
        compose.onNodeWithText("View proposal").performClick()
        compose.onNodeWithText("Proposal").assertIsSelected()
        compose.onNodeWithText("Conversation").performClick()
        compose.onAllNodes(unread).assertCountEquals(0)
        compose.onNodeWithText("View proposal").assertIsDisplayed()
        compose.runOnIdle { messages.value = messages.value + Message.assistantMessage("Same plan") }
        compose.onAllNodes(unread).assertCountEquals(0)
        compose.runOnIdle { tasks.value = listOf(tasks.value.single().copy(scheduledDate = "2026-11-04")) }
        compose.onNode(unread).assertIsDisplayed()
        compose.onNodeWithText("Proposal").performClick()
        // Cambios recibidos mientras Propuesta está visible se reconocen al mostrarse.
        compose.runOnIdle { tasks.value = listOf(tasks.value.single().copy(notes = "Updated instructions")) }
        compose.onNodeWithText("Conversation").performClick()
        compose.onAllNodes(unread).assertCountEquals(0)
    }

    @Test fun loadingAndClearingHideTheActionAndIndicator() {
        loading.value = true
        tasks.value = listOf(Task(title = "Draft"))
        show()
        compose.onNodeWithText("View proposal").assertDoesNotExist()
        compose.onAllNodes(unread).assertCountEquals(0)
        compose.runOnIdle { loading.value = false }
        compose.onNodeWithText("View proposal").assertIsDisplayed()
        compose.runOnIdle { tasks.value = emptyList() }
        compose.onNodeWithText("View proposal").assertDoesNotExist()
        compose.onAllNodes(unread).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "es-w320dp-h900dp")
    fun compactHeaderKeepsSpanishProposalIndicatorAndTabAccess() {
        tasks.value = listOf(Task(title = "Estudiar"))
        show(height = 240)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            "Propuesta nueva sin consultar")).assertIsDisplayed()
        compose.onNodeWithText("Propuesta").performClick().assertIsSelected()
        compose.onNodeWithText("Conversación").performClick()
        compose.onAllNodes(unread).assertCountEquals(0)
    }
}
