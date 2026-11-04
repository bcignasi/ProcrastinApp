package com.ibc.procrastinapp.ui.tasklist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ibc.procrastinapp.ui.tasklist.elements.TaskListTopBar
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es")
class GoalsNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completedIsAFilterWithinPendingAndGoalsIsAnIndependentTab() {
        compose.setContent {
            var filter by rememberSaveable { mutableStateOf(TaskListQueryType.TODAY) }
            var pendingFilter by rememberSaveable { mutableStateOf(TaskListQueryType.ALL) }
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                TaskListTopBar(
                    currentListType = filter,
                    pendingFilter = pendingFilter,
                    onOpenSettings = {},
                    onCurrentListTypeChange = {
                        if (it == TaskListQueryType.ALL || it == TaskListQueryType.COMPLETED) pendingFilter = it
                        filter = it
                    }
                )
            }
        }
        compose.onNodeWithText("Hechas").assertDoesNotExist()
        compose.onNodeWithText("Completadas").assertDoesNotExist()
        compose.onNodeWithText("Metas").performClick().assertIsSelected()
        compose.onNodeWithText("Pendientes").performClick().assertIsSelected()
        compose.onNodeWithText("Por hacer").assertIsSelected()
        compose.onNodeWithText("Completadas").performClick().assertIsSelected()
        compose.onNodeWithText("Pendientes").assertIsSelected()
        compose.onNodeWithText("Hoy").performClick()
        compose.onNodeWithText("Completadas").assertDoesNotExist()
        compose.onNodeWithText("Pendientes").performClick()
        compose.onNodeWithText("Completadas").assertIsSelected()
    }
}
