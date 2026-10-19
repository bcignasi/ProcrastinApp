package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.settings.SettingsScreen
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "en")
class AssistantScreenTabsTest {
    @get:Rule
    val compose = createComposeRule()

    private val model = mockk<AssistantViewModel>(relaxed = true)
    private val quotes = mockk<QuoteViewModel>(relaxed = true)
    private val saving = MutableStateFlow(false)

    @Before
    fun setUp() {
        every { model.uiState } returns MutableStateFlow(
            AssistantState(
                messages = listOf(Message.userMessage("Quiero preparar una entrevista")),
                tasks = listOf(Task(id = 1, title = "Ensayar preguntas"))
            )
        )
        every { model.lastResponse } returns MutableStateFlow(null)
        every { model.isSaving } returns saving
        every { model.commitFinished } returns MutableSharedFlow()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun showScreen(darkTheme: Boolean = true) {
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = darkTheme, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "assistant") {
                        composable("assistant") {
                            AssistantScreen(navController = nav, assistantViewModel = model, quoteViewModel = quotes)
                        }
                        composable("settings") {
                            SettingsScreen(darkTheme, false, false, {}, { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    @Test
    fun conversationIsTheDefaultAndSwitchingTabsKeepsTheDraftAndContents() {
        showScreen()
        compose.onNodeWithText("Conversation").assertIsSelected()
        compose.onNodeWithText("Quiero preparar una entrevista").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("Mi borrador")

        compose.onNodeWithText("Proposal").performClick().assertIsSelected()
        compose.onNodeWithText("Ensayar preguntas", substring = true).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("Mi borrador")
        compose.onNodeWithText("Conversation").performClick().assertIsSelected()
        compose.onNodeWithText("Quiero preparar una entrevista").assertIsDisplayed()
        verify(exactly = 0) { model.startNewSession() }
    }

    @Test
    fun settingsReturnsToTheSameProposalAndDraftInLightMode() {
        showScreen(darkTheme = false)
        compose.onNodeWithText("Proposal").performClick().assertIsSelected()
        compose.onNode(hasSetTextAction()).performTextInput("Conservar este borrador")
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithContentDescription(context.getString(R.string.settings_open)).performClick()
        compose.onNodeWithText(context.getString(R.string.settings_appearance)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.settings_back)).performClick()
        compose.onNodeWithText("Proposal").assertIsSelected()
        compose.onNodeWithText("Ensayar preguntas", substring = true).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("Conservar este borrador")
        verify(exactly = 0) { model.startNewSession() }
    }

    @Test
    fun restartIsAccessibleFromTheMenuAndDisabledWhileSaving() {
        showScreen()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Restart conversation").performClick()
        verify(exactly = 1) { model.startNewSession() }

        compose.runOnIdle { saving.value = true }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Restart conversation").assertIsNotEnabled()
        verify(exactly = 1) { model.startNewSession() }
    }
}
