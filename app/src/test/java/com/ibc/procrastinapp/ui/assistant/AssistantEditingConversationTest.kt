package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.data.service.MessageStorage
import com.ibc.procrastinapp.data.service.quotes.QuoteAIService
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es-w320dp-h900dp")
class AssistantEditingConversationTest {
    @get:Rule val compose = createComposeRule()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ViewModelStore()

    @After
    fun tearDown() {
        store.clear()
        applicationScope.cancel()
        unmockkAll()
    }

    @Test
    fun openingAnAgendaTaskShowsReadableSpanishAndKeepsTheInternalProposal() {
        checkEditingConversation(
            conversationTab = "Conversación",
            proposalTab = "Propuesta",
            guidance = "Tu plan está disponible en Propuesta. Puedes pedirme cambios aquí."
        )
    }

    @Test
    @Config(qualifiers = "en-w320dp-h900dp")
    fun openingAnAgendaTaskShowsReadableEnglishAndKeepsTheInternalProposal() {
        checkEditingConversation(
            conversationTab = "Conversation",
            proposalTab = "Proposal",
            guidance = "Your plan is available in Proposal. You can ask me for changes here."
        )
    }

    private fun checkEditingConversation(conversationTab: String, proposalTab: String, guidance: String) {
        val goal = Task(
            id = 42,
            title = "Sacar el carnet de conducir",
            subtasks = listOf(Task(title = "Elegir autoescuela"), Task(title = "Practicar tests"))
        )
        // El límite de lectura de Agenda se simula; sesión, serialización, parser y UI son reales.
        val tasks = mockk<TaskRepository>(relaxed = true)
        coEvery { tasks.getTask(goal.id) } returns goal
        val api = mockk<AIService>()
        val storage = object : MessageStorage {
            override suspend fun loadMessages(): List<Message>? = null
            override suspend fun saveMessages(messages: List<Message>) = Unit
        }
        lateinit var repository: AssistantRepository
        compose.runOnIdle {
            repository = AssistantRepository(ChatAIService(storage, api, applicationScope), applicationScope)
        }
        val model = ViewModelProvider(store, viewModelFactory {
            initializer { AssistantViewModel(repository, tasks, applicationScope) }
        })[AssistantViewModel::class.java]
        val quotes = QuoteViewModel(mockk<QuoteAIService>(relaxed = true))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                AssistantScreen(
                    navController = rememberNavController(), assistantViewModel = model,
                    quoteViewModel = quotes, taskIdsToEdit = listOf(goal.id)
                )
            }
        }
        compose.waitUntil(10_000) { repository.tasks.value.isNotEmpty() }
        val raw = repository.messages.value.last { it.isAssistant }.content
        assertTrue(raw.contains("\"propuesta\""))
        assertTrue(raw.contains("\"subtasks\""))
        assertEquals(goal.subtasks.map { it.title }, repository.tasks.value.single().subtasks.map { it.title })
        compose.onNodeWithText(conversationTab).assertIsSelected()
        compose.onNodeWithText(raw).assertDoesNotExist()
        compose.onNodeWithText(guidance).assertIsDisplayed()
        compose.onNodeWithText(proposalTab).performClick()
        compose.onNodeWithText(goal.title, substring = true).assertIsDisplayed()
        compose.onNodeWithText(conversationTab).performClick()
        compose.onNodeWithText(guidance).assertIsDisplayed()
        assertEquals(raw, repository.messages.value.last { it.isAssistant }.content)
        coVerify(exactly = 0) { api.sendMessage(any()) }
    }
}
