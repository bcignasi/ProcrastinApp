package com.ibc.procrastinapp.ui.common

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.tasklist.elements.TaskItemSelectable
import com.ibc.procrastinapp.ui.tasklist.elements.TaskList
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class FlatTaskRowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun checkboxCompletesAndReopensWithoutOpeningOrSelectingTheRow() {
        val task = mutableStateOf(Task(id = 42, title = "Practicar teoría"))
        val completedIds = mutableListOf<Long>()
        var opened = 0
        var selected = 0
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    TaskItemSelectable(
                        task = task.value,
                        presentation = TaskPresentation.FLAT,
                        isSelected = false,
                        onClick = { opened++ },
                        onLongClick = { selected++ },
                        onComplete = {
                            completedIds.add(it.id)
                            task.value = it.copy(completed = !it.completed)
                        }
                    )
                }
            }
        }

        compose.onNode(isToggleable()).assertIsOff().performTouchInput { click() }.assertIsOn()
        compose.onNode(isToggleable()).performTouchInput { click() }.assertIsOff()
        compose.runOnIdle {
            assertEquals(listOf(42L, 42L), completedIds)
            assertEquals(0, opened)
            assertEquals(0, selected)
        }
        compose.onNodeWithText("Practicar teoría").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun aSubtaskCanBeCompletedWithoutCompletingItsParent() {
        val child = Task(id = 8, title = "Test de señales")
        val task = mutableStateOf(Task(id = 7, title = "Carnet", subtasks = listOf(child)))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                TaskItemContent(
                    task = task.value,
                    presentation = TaskPresentation.FLAT,
                    actionsEnabled = false,
                    onComplete = { toggled ->
                        task.value = task.value.copy(subtasks = listOf(toggled.copy(completed = !toggled.completed)))
                    }
                )
            }
        }
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithContentDescription(context.getString(R.string.tasklist_expand_subtasks_cd)).performClick()
        compose.onAllNodes(isToggleable()).assertCountEquals(2)
        compose.onAllNodes(isToggleable())[1].assertIsOff().performClick().assertIsOn()
        compose.runOnIdle {
            assertEquals(false, task.value.completed)
            assertEquals(true, task.value.subtasks.single().completed)
        }
        compose.onAllNodes(isToggleable())[0].assertIsOff()
    }

    @Test
    fun cardsKeepTheExistingTitleAndHaveNoCompletionCheckbox() {
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                TaskItemContent(task = Task(title = "Tarjeta original"), actionsEnabled = false)
            }
        }
        compose.onNodeWithText("Tarjeta original").assertExists()
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
    }

    @Test
    fun metadataKeepsUrgencyWithoutDatesAndRefreshesWhenTheDayChanges() {
        val today = LocalDate.of(2026, 10, 19)
        val referenceDate = mutableStateOf(today)
        val task = mutableStateOf(Task(id = 42, title = "Presentar solicitud", notify = "$today 16:30", priority = 3))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                TaskList(
                    tasks = listOf(task.value),
                    selectedTaskIds = emptySet(),
                    onTaskClick = {},
                    onTaskLongClick = {},
                    showInlineActionsForId = null,
                    onInlineComplete = {},
                    onInlineDelete = {},
                    presentation = TaskPresentation.FLAT,
                    referenceDate = referenceDate.value
                )
            }
        }
        compose.onNodeWithText("16:30", substring = true).assertExists()
        compose.runOnIdle { referenceDate.value = today.plusDays(1) }
        compose.onNodeWithText("2026", substring = true).assertExists()
        compose.runOnIdle { task.value = task.value.copy(notify = "") }
        val context = RuntimeEnvironment.getApplication()
        val priorityLabel = context.getString(R.string.task_priority_urgent).uppercase(context.resources.configuration.locales[0])
        compose.onNodeWithText(priorityLabel).assertExists()
    }

    @Test
    fun flatRowShowsOnlyTheReminderDateAndUsesAPriorityBadge() {
        val today = LocalDate.of(2026, 10, 19)
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    TaskItemContent(
                        task = Task(id = 42, title = "Practicar teoría", deadline = "$today 16:30", notify = "$today 16:15", priority = 2),
                        actionsEnabled = false,
                        presentation = TaskPresentation.FLAT,
                        referenceDate = today
                    )
                }
            }
        }
        compose.onNodeWithText("16:15").assertExists()
        compose.onNodeWithText("16:30", substring = true).assertDoesNotExist()
        val context = RuntimeEnvironment.getApplication()
        val label = context.getString(R.string.task_priority_high).uppercase(context.resources.configuration.locales[0])
        compose.onNodeWithText(label).assertExists()
        compose.onNodeWithContentDescription(context.getString(R.string.notification_cd)).assertExists()
    }
}
