package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.AIServiceError
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
@Config(sdk = [30], application = Application::class, qualifiers = "es-w400dp-h900dp")
class StructuredAssistantErrorTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun errorsAreVisibleInBothTabsInSpanishWithoutHidingTheExistingPlan() = checkErrors()

    @Test
    @Config(qualifiers = "en-w400dp-h900dp")
    fun errorsAreVisibleInBothTabsInEnglishWithoutHidingTheExistingPlan() = checkErrors()

    private fun checkErrors() {
        val model = mockk<AssistantViewModel>(relaxed = true)
        val initial = AssistantState(
            messages = listOf(Message.assistantMessage("Seguimos con tu plan")),
            tasks = listOf(Task(title = "Aprobar el teórico"))
        )
        val state = MutableStateFlow(initial)
        every { model.uiState } returns state
        every { model.lastResponse } returns MutableStateFlow(null)
        every { model.isSaving } returns MutableStateFlow(false)
        every { model.commitFinished } returns MutableSharedFlow()
        val quotes = QuoteViewModel(mockk<QuoteAIService>(relaxed = true))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                AssistantScreen(
                    navController = rememberNavController(), assistantViewModel = model, quoteViewModel = quotes
                )
            }
        }
        val context = RuntimeEnvironment.getApplication()
        val errors = listOf(
            AIServiceError.IncompleteResponse to R.string.error_ai_incomplete_response,
            AIServiceError.RefusedResponse to R.string.error_ai_refused_response,
            AIServiceError.InvalidStructuredResponse to R.string.error_ai_invalid_structured_response
        )
        for ((error, resource) in errors) {
            compose.runOnIdle { state.value = initial.copy(chatAIServiceError = error) }
            val text = context.getString(resource)
            compose.onNodeWithText(text).assertIsDisplayed()
            compose.onNodeWithText("Seguimos con tu plan").assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.assistant_type_Tasks)).performClick()
            compose.onNodeWithText(text).assertIsDisplayed()
            compose.onNodeWithText("Aprobar el teórico", substring = true).assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.assistant_type_Messages)).performClick()
        }
    }
}
