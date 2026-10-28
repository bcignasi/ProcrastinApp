package com.ibc.procrastinapp.ui.assistant

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.service.quotes.QuoteAIService
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "en-w320dp-h900dp")
class AssistantScreenTabsTest {
    @get:Rule
    val compose = createComposeRule()

    private val model = mockk<AssistantViewModel>(relaxed = true)
    private val quotes = mockk<QuoteViewModel>(relaxed = true)
    private val saving = MutableStateFlow(false)
    private var pixelsPerDp = 1f

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

    private fun showScreen(
        darkTheme: Boolean = true,
        availableHeight: State<Int>? = null,
        fontScale: Float = 1f,
        quoteModel: QuoteViewModel = quotes
    ) {
        compose.setContent {
            pixelsPerDp = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, fontScale)) {
                ProcAppMVVMTheme(darkTheme = darkTheme, dynamicColor = false) {
                    Box(Modifier.width(320.dp).then(
                        availableHeight?.let { Modifier.height(it.value.dp) } ?: Modifier
                    )) {
                        val nav = rememberNavController()
                        NavHost(navController = nav, startDestination = "assistant") {
                            composable("assistant") {
                                AssistantScreen(navController = nav, assistantViewModel = model, quoteViewModel = quoteModel)
                            }
                            composable("settings") {
                                SettingsScreen(darkTheme, false, false, {}, { nav.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkEmptyGuidanceAfterTabChanges(darkTheme: Boolean) {
        val state = MutableStateFlow(AssistantState())
        every { model.uiState } returns state
        // Keep the real quote state across tab changes, with no network calls.
        val quoteModel = QuoteViewModel(mockk<QuoteAIService>(relaxed = true))
        showScreen(darkTheme = darkTheme, quoteModel = quoteModel)
        val context = RuntimeEnvironment.getApplication()
        val intro = context.getString(R.string.assistant_intro_text)
        val proposalIntro = context.getString(R.string.assistant_proposal_empty_text)
        val conversation = context.getString(R.string.assistant_type_Messages)
        val proposal = context.getString(R.string.assistant_type_Tasks)
        compose.onNodeWithText(intro).assertIsDisplayed()

        repeat(4) {
            compose.onNodeWithText(proposal).performClick()
            compose.onNodeWithText(proposalIntro).assertIsDisplayed()
            compose.onNodeWithText(conversation).performClick()
            compose.onNodeWithText(intro).assertIsDisplayed()
        }
        compose.onNode(hasSetTextAction()).performTextInput("Mi objetivo")
        compose.onNodeWithText(intro).assertIsDisplayed()

        compose.runOnIdle {
            state.value = AssistantState(
                messages = listOf(Message.userMessage("Quiero preparar una entrevista")),
                tasks = listOf(Task(id = 1, title = "Ensayar preguntas"))
            )
        }
        compose.onNodeWithText(intro).assertDoesNotExist()
        compose.onNodeWithText("Quiero preparar una entrevista").assertIsDisplayed()
        compose.onNodeWithText(proposal).performClick()
        compose.onNodeWithText(proposalIntro).assertDoesNotExist()
        compose.onNodeWithText("Ensayar preguntas", substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyGuidanceSurvivesRepeatedTabChangesAndTypingInEnglish() =
        checkEmptyGuidanceAfterTabChanges(darkTheme = true)

    @Test
    @Config(qualifiers = "es-w320dp-h900dp")
    fun emptyGuidanceSurvivesRepeatedTabChangesAndTypingInSpanish() =
        checkEmptyGuidanceAfterTabChanges(darkTheme = false)

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
    fun viewProposalButtonOpensTheExistingTabAndKeepsTheInputDraft() {
        showScreen()
        compose.onNode(hasSetTextAction()).performTextInput("Mover el lunes")
        compose.onNodeWithText("View proposal").assertIsDisplayed().performClick()
        compose.onNodeWithText("Proposal").assertIsSelected()
        compose.onNodeWithText("Ensayar preguntas", substring = true).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("Mover el lunes")
        compose.onNodeWithText("Conversation").performClick()
        compose.onNodeWithText("View proposal").assertIsDisplayed()
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

    private fun assertInputHasEnoughVisibleHeight() {
        val heightDp = compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.height / pixelsPerDp
        assertTrue("El campo de escritura tiene solo $heightDp dp visibles", heightDp >= 48f)
    }

    private fun checkWritingAndSending(availableHeight: Int) {
        showScreen(availableHeight = mutableStateOf(availableHeight))
        compose.onNode(hasSetTextAction()).performTextInput("Objetivo de prueba")
        assertInputHasEnoughVisibleHeight()
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithContentDescription(context.getString(R.string.assistant_send_content_description))
            .assertIsDisplayed().performTouchInput { click() }
        verify(exactly = 1) { model.sendMessage("Objetivo de prueba") }
    }

    @Test
    fun writingAndSendingRemainUsableWith240DpAvailable() = checkWritingAndSending(240)

    @Test
    fun writingAndSendingRemainUsableWith180DpAvailable() = checkWritingAndSending(180)

    @Test
    fun compactHeaderKeepsTheSelectedTabDraftAndSettingsWithLargeText() {
        val height = mutableStateOf(640)
        showScreen(availableHeight = height, fontScale = 1.5f)
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithContentDescription(context.getString(R.string.settings_open)).assertIsDisplayed()
        compose.onNodeWithText("Proposal").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Borrador al cambiar de tamaño")

        compose.runOnIdle { height.value = 240 }
        compose.onNodeWithText("Proposal").assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextContains("Borrador al cambiar de tamaño")
        assertInputHasEnoughVisibleHeight()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText(context.getString(R.string.settings_title)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.settings_back)).performClick()
        compose.onNodeWithText("Proposal").assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextContains("Borrador al cambiar de tamaño")

        compose.runOnIdle { height.value = 640 }
        compose.onNodeWithContentDescription(context.getString(R.string.settings_open)).assertIsDisplayed()
        verify(exactly = 0) { model.startNewSession() }
    }
}
