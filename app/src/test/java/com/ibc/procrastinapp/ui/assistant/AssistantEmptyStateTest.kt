package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.service.quotes.QuoteAIService
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es-w320dp-h900dp")
class AssistantEmptyStateTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun independentListsAndReturningToEmptyKeepTheCorrectGuidance() {
        val model = mockk<AssistantViewModel>(relaxed = true)
        val state = MutableStateFlow(AssistantState())
        every { model.uiState } returns state
        every { model.lastResponse } returns MutableStateFlow(null)
        every { model.isSaving } returns MutableStateFlow(false)
        every { model.commitFinished } returns MutableSharedFlow()
        val quotes = QuoteViewModel(mockk<QuoteAIService>(relaxed = true))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                AssistantScreen(
                    navController = rememberNavController(),
                    assistantViewModel = model,
                    quoteViewModel = quotes
                )
            }
        }
        val context = RuntimeEnvironment.getApplication()
        val intro = context.getString(R.string.assistant_intro_text)
        val proposalIntro = context.getString(R.string.assistant_proposal_empty_text)
        val conversation = context.getString(R.string.assistant_type_Messages)
        val proposal = context.getString(R.string.assistant_type_Tasks)

        compose.onNodeWithText(intro).assertIsDisplayed()
        compose.runOnIdle {
            state.value = AssistantState(messages = listOf(Message.userMessage("Objetivo")))
        }
        compose.onNodeWithText(intro).assertDoesNotExist()
        compose.onNodeWithText("Objetivo").assertIsDisplayed()
        compose.onNodeWithText(proposal).performClick()
        compose.onNodeWithText(proposalIntro).assertIsDisplayed()

        compose.runOnIdle {
            state.value = AssistantState(tasks = listOf(Task(id = 1, title = "Paso")))
        }
        compose.onNodeWithText(proposalIntro).assertDoesNotExist()
        compose.onNodeWithText("Paso", substring = true).assertIsDisplayed()
        compose.onNodeWithText(conversation).performClick()
        compose.onNodeWithText(intro).assertIsDisplayed()

        compose.runOnIdle { state.value = AssistantState() }
        repeat(4) {
            compose.onNodeWithText(proposal).performClick()
            compose.onNodeWithText(proposalIntro).assertIsDisplayed()
            compose.onNodeWithText(conversation).performClick()
            compose.onNodeWithText(intro).assertIsDisplayed()
        }
    }
}
