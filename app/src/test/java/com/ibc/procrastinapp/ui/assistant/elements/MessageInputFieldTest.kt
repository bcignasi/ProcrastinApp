package com.ibc.procrastinapp.ui.assistant.elements

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.theme.ProcAppMVVMTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, qualifiers = "es-w320dp-h900dp")
class MessageInputFieldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun busyInputBlocksTouchAndKeepsTheMultilineDraftUntilSending() {
        val busy = mutableStateOf(false)
        val sent = mutableListOf<String>()
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.5f)) {
                ProcAppMVVMTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        MessageInputField(onSendMessage = { sent.add(it) }, isLoading = busy.value)
                    }
                }
            }
        }
        val sendDescription = RuntimeEnvironment.getApplication()
            .getString(R.string.assistant_send_content_description)
        val draft = "Mi objetivo\nSegundo paso"
        compose.onNodeWithContentDescription(sendDescription).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput(draft)
        val sendPosition = compose.onNodeWithContentDescription(sendDescription)
            .fetchSemanticsNode().boundsInRoot.center

        compose.runOnIdle { busy.value = true }
        compose.onNode(hasSetTextAction()).assertTextContains(draft)
        compose.onRoot().performTouchInput { click(sendPosition) }
        compose.runOnIdle {
            assertEquals(emptyList<String>(), sent)
            busy.value = false
        }

        compose.onNodeWithContentDescription(sendDescription).performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(draft), sent) }
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
        compose.onNodeWithContentDescription(sendDescription).assertIsNotEnabled()
    }
}
