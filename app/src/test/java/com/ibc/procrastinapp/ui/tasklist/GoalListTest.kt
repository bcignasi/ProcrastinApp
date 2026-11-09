package com.ibc.procrastinapp.ui.tasklist

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.tasklist.elements.GoalList
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es")
class GoalListTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingGoalRevealsFullDescriptionAndAll28SessionsWithoutOpeningEditor() {
        val notes = "Practicar de forma consistente y revisar el método.\n\nLeer 10 minutos y meditar 20, con una revisión al finalizar las cuatro semanas."
        val goal = Task(id = 1, title = "Meditar", notes = notes,
            subtasks = (1..28).map { Task(id = it + 1L, title = "Sesión $it") })
        val edits = mutableListOf<Long>()
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                GoalList(listOf(goal), emptySet(), edits::add, {}, {}, LocalDate.of(2026, 11, 4))
            }
        }
        compose.onNodeWithText(notes).assertDoesNotExist()
        compose.onNodeWithText("Sesión 1").assertDoesNotExist()
        compose.onNodeWithText("Meditar").performClick()
        compose.onNodeWithText(notes).assertIsDisplayed()
        compose.onNodeWithText("Sesión 28").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<Long>(), edits) }
        compose.onNodeWithContentDescription("Editar meta: Meditar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(1L), edits) }
        compose.onNodeWithText("Meditar").performScrollTo().performClick()
        compose.onNodeWithText(notes).assertDoesNotExist()
        compose.onNodeWithText("Sesión 1").assertDoesNotExist()
    }

    @Test fun goalWithoutSubtasksStillExpandsAndRestoresItsFullDetails() {
        val restore = StateRestorationTester(compose)
        val goal = Task(id = 42, title = "Aprender", notes = "Descripción sin subtareas", completed = true)
        restore.setContent {
            ProcAppMVVMTheme(darkTheme = false, dynamicColor = false) {
                GoalList(listOf(goal), emptySet(), {}, {}, {}, LocalDate.of(2026, 11, 4))
            }
        }
        compose.onNodeWithText("Completadas").assertExists()
        compose.onNodeWithText(goal.notes).assertDoesNotExist()
        compose.onNodeWithText(goal.title).performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(goal.notes).assertIsDisplayed()
        compose.onNodeWithText(goal.title).performClick()
        compose.onNodeWithText(goal.notes).assertDoesNotExist()
    }

    @Test fun completingASessionKeepsItsGoalPendingAndExpanded() {
        val goal = mutableStateOf(Task(id = 1, title = "Meditar", notes = "Método completo",
            subtasks = listOf(Task(id = 2, title = "Sesión"))))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                GoalList(listOf(goal.value), emptySet(), {}, {}, { child ->
                    goal.value = goal.value.copy(subtasks = listOf(child.copy(completed = !child.completed)))
                }, LocalDate.of(2026, 11, 4))
            }
        }
        compose.onNodeWithText("Meditar").performClick()
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Método completo").assertIsDisplayed()
        compose.runOnIdle { assertFalse(goal.value.completed) }
        compose.onNode(isToggleable()).performClick().assertIsOff()
    }
}
