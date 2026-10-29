package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es-w320dp-h900dp")
class ProposalTaskPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    @Test
    fun tenStepsAreAvailableWithoutExpandingTheGoalAndProgressUsesTheirActualState() {
        val task = Task(title = "Carnet de conducir", subtasks = List(10) {
            Task(title = "Paso ${it + 1} del carnet", completed = it == 0)
        })
        show(listOf(task))

        compose.onNodeWithText(task.title).assertIsDisplayed()
        compose.onNodeWithText("1 de 10 pasos").assertIsDisplayed()
        compose.onNode(progress).assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.1f, 0f..1f)
        ))
        compose.onNodeWithText("Paso 1 del carnet").assertIsDisplayed()
        compose.onNodeWithText("Paso 10 del carnet").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun nestedStepsKeepTheirHierarchyEvenWhenEveryTaskHasTheNewTaskIdZero() {
        val task = Task(title = "Carnet", subtasks = listOf(
            Task(title = "Teoría", completed = true, subtasks = listOf(
                Task(title = "Estudiar señales"), Task(title = "Practicar tests")
            )),
            Task(title = "Práctica")
        ))
        show(listOf(task))

        compose.onNodeWithText("1 de 2 pasos").assertIsDisplayed()
        compose.onNodeWithText("1.1").assertIsDisplayed()
        compose.onNodeWithText("Estudiar señales").assertIsDisplayed()
        compose.onNodeWithText("1.2").assertIsDisplayed()
        compose.onNodeWithText("Practicar tests").assertIsDisplayed()
        compose.onNodeWithText("Práctica").assertIsDisplayed()
    }

    @Test
    fun completedAndPendingRowsAreReadOnlyAndTouchDoesNotChangeTheirState() {
        val task = Task(title = "Carnet", subtasks = listOf(
            Task(title = "Elegir autoescuela", completed = true), Task(title = "Hacer un test")
        ))
        show(listOf(task))

        val done = compose.onNodeWithText("Elegir autoescuela")
        val pending = compose.onNodeWithText("Hacer un test")
        done.assertHasNoClickAction().assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Completada. Solo lectura."
        ))
        pending.assertHasNoClickAction().assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Pendiente. Solo lectura."
        ))
        done.performTouchInput { click() }
        pending.performTouchInput { click() }
        compose.onNodeWithText("1 de 2 pasos").assertIsDisplayed()
        assertEquals(listOf(true, false), task.subtasks.map { it.completed })
    }

    @Test
    fun standaloneTaskShowsOnlyItsReminderAndDoesNotInventAProgressBar() {
        val task = Task(
            title = "Comprar el pan", notify = "${LocalDate.now()} 19:00",
            deadline = "${LocalDate.now()} 21:00"
        )
        show(listOf(task))

        compose.onNodeWithText(task.title).assertIsDisplayed()
        compose.onNodeWithText("19:00").assertIsDisplayed()
        compose.onNodeWithText("21:00", substring = true).assertDoesNotExist()
        compose.onAllNodes(progress).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "en-w320dp-h900dp")
    fun englishSingularAndDetailsRemainAccessibleWithLargeTextInLightTheme() {
        val task = Task(title = "Prepare for a software engineering interview", subtasks = listOf(
            Task(title = "Practise explaining the most recent project in detail", completed = true,
                notes = "Explain one concrete decision", periodicity = "Every Monday",
                deadline = "2030-02-03 18:00", notify = "${LocalDate.now()} 16:30", priority = 2)
        ))
        show(listOf(task), largeText = true, darkTheme = false)

        compose.onNodeWithText("1 of 1 step").assertIsDisplayed()
        compose.onNodeWithText("16:30").assertIsDisplayed()
        compose.onNodeWithText(task.subtasks.single().notes).assertIsDisplayed()
        compose.onNodeWithText("Show more").performScrollTo().performClick()
        compose.onNodeWithText(task.subtasks.single().notes).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Repetition: Every Monday").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Deadline:", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Priority:", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun notesWithoutMetadataOnlyOfferExpansionWhenTheyOverflow() {
        val task = mutableStateOf(Task(title = "Sesión", notes = "Haz un test de 30 min."))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                ProposalTaskDetails(task.value, LocalDate.now())
            }
        }
        compose.onNodeWithText(task.value.notes).assertIsDisplayed()
        compose.onNodeWithText("Ver más").assertDoesNotExist()
        compose.runOnIdle { task.value = task.value.copy(notes = "Haz un test.\nRevisa errores.\nRepasa señales.") }
        compose.onNodeWithText("Ver más").performClick()
        compose.onNodeWithText("Ver menos").assertIsDisplayed()
        compose.runOnIdle { task.value = task.value.copy(notes = "Nueva instrucción breve.") }
        compose.onNodeWithText("Nueva instrucción breve.").assertIsDisplayed()
        compose.onNodeWithText("Ver menos").assertDoesNotExist()
        compose.onNodeWithText("Ver más").assertDoesNotExist()
    }

    @Test
    fun longInstructionsHaveATwoLinePreviewAndCanBeExpandedAndCollapsedWithoutLosingContent() {
        val notes = "Haz un test durante 30 minutos en el recurso acordado.\n" +
            "Después revisa los errores y anota las señales que necesitas repasar.\n".repeat(5)
        show(listOf(Task(title = "Test de teoría", notes = notes, periodicity = "Lunes y jueves")))
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(notes).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(2, layouts.last().lineCount)
        assertTrue(layouts.last().hasVisualOverflow)
        compose.onNodeWithText("Repetición: Lunes y jueves").assertDoesNotExist()
        compose.onNodeWithText("Ver más").performClick()
        layouts.clear()
        compose.onNodeWithText(notes).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.last().lineCount > 2)
        compose.onNodeWithText("Repetición: Lunes y jueves").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ver menos").performScrollTo().performClick()
        compose.onNodeWithText("Repetición: Lunes y jueves").assertDoesNotExist()
    }

    @Test
    fun scheduledDayIsVisibleWithoutExpandingDetailsOrEnablingReminder() {
        show(listOf(Task(title = "Teórico", subtasks = listOf(
            Task(title = "Test", scheduledDate = "2030-02-03", notes = "Instrucciones privadas")
        ))))
        compose.onNodeWithText("Planificado:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("2030", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Instrucciones privadas").assertIsDisplayed()
        compose.onNodeWithText("00:00", substring = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en-w320dp-h900dp")
    fun scheduledDayAndReminderHaveSeparateLabelsInEnglish() {
        show(listOf(Task(title = "Session", scheduledDate = "2030-02-03",
            notify = "${LocalDate.now()} 19:00")))
        compose.onNodeWithText("Scheduled:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("19:00").assertIsDisplayed()
    }

    private fun show(tasks: List<Task>, largeText: Boolean = false, darkTheme: Boolean = true) {
        val quotes = mockk<QuoteViewModel>(relaxed = true)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else 1f)) {
                ProcAppMVVMTheme(darkTheme = darkTheme, dynamicColor = false) {
                    AssistantTasksViewList(tasks, Modifier.fillMaxSize(), quotes)
                }
            }
        }
    }
}
