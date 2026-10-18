package com.ibc.procrastinapp.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.tasklist.TaskListQueryType
import com.ibc.procrastinapp.ui.tasklist.elements.TaskListTopBar
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun narrowAgendaKeepsTheFilterArrowAndSettingsReachable() {
        var openedSettings = false
        compose.setContent {
            ProcAppMVVMTheme {
                Box(Modifier.width(320.dp)) {
                    TaskListTopBar(
                        currentListType = TaskListQueryType.ALL,
                        onCurrentListTypeChange = {},
                        onGoToAssistant = {},
                        onOpenSettings = { openedSettings = true }
                    )
                }
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithContentDescription(context.getString(R.string.title_dropdown_change_type_cd))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.settings_open))
            .assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(openedSettings) }
    }

    @Test
    fun switchCanChangeFromLightToDarkAndBack() {
        compose.setContent {
            var darkTheme by remember { mutableStateOf(false) }
            ProcAppMVVMTheme(darkTheme = darkTheme, dynamicColor = false) {
                SettingsScreen(
                    darkTheme = darkTheme,
                    isSaving = false,
                    saveFailed = false,
                    onDarkThemeChange = { darkTheme = it },
                    onBack = {}
                )
            }
        }

        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        compose.onNode(isToggleable()).performClick().assertIsOff()
    }

    @Test
    fun savingDisablesTheSwitchAndBackRemainsAvailable() {
        var returned = false
        compose.setContent {
            ProcAppMVVMTheme {
                SettingsScreen(
                    darkTheme = true,
                    isSaving = true,
                    saveFailed = false,
                    onDarkThemeChange = {},
                    onBack = { returned = true }
                )
            }
        }

        compose.onNode(isToggleable()).assertIsNotEnabled()
        val backLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(R.string.settings_back)
        compose.onNodeWithContentDescription(backLabel).performClick()
        compose.runOnIdle { assertTrue(returned) }
    }
}
