package com.ibc.procrastinapp.ui.tasklist

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ibc.procrastinapp.R
import androidx.navigation.compose.rememberNavController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es-w400dp-h900dp")
class TaskDeletionWarningTest {
    @get:Rule val compose = createComposeRule()

    @Test fun warningIsVisibleInSpanishAfterDeletingTheLastTask() = checkWarning()

    @Test @Config(qualifiers = "en-w400dp-h900dp")
    fun warningIsVisibleInEnglishAfterDeletingTheLastTask() = checkWarning()

    private fun checkWarning() {
        val model = mockk<TaskListViewModel>(relaxed = true)
        every { model.uiState } returns MutableStateFlow(TaskListUiState(result = TaskListResult.DeleteRemindersFailed))
        compose.setContent {
            ProcAppMVVMTheme(darkTheme = true, dynamicColor = false) {
                TaskListScreen(navController = rememberNavController(), viewModel = model)
            }
        }
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithText(context.getString(R.string.tasklist_delete_reminders_failed)).assertIsDisplayed()
    }
}
