package com.ibc.procrastinapp.ui.navigation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class MainNavigationTest {
    private lateinit var navigation: NavHostController

    @Before
    fun setUp() {
        navigation = NavHostController(RuntimeEnvironment.getApplication()).apply {
            setViewModelStore(ViewModelStore())
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = "splash") {
                composable("splash") {}
                composable(AGENDA_ROUTE) {}
                composable("settings") {}
                assistantDestinations { _, _ -> }
            }
            navigate(AGENDA_ROUTE) { popUpTo("splash") { inclusive = true } }
        }
    }

    @Test
    fun tabSwitchesKeepTheAgendaFilterAndAssistantState() {
        val agenda = navigation.currentBackStackEntry!!
        agenda.savedStateHandle["filter"] = "COMPLETED"
        navigation.openMainDestination(ASSISTANT_ROUTE)
        val assistant = navigation.currentBackStackEntry!!
        assistant.savedStateHandle["draft"] = "Preparar entrevista"
        val session = ViewModelProvider(assistant)[SessionState::class.java]

        navigation.openMainDestination(AGENDA_ROUTE)
        assertEquals(agenda.id, navigation.currentBackStackEntry!!.id)
        assertEquals("COMPLETED", navigation.currentBackStackEntry!!.savedStateHandle.get<String>("filter"))

        navigation.openMainDestination(ASSISTANT_ROUTE)
        val restored = navigation.currentBackStackEntry!!
        assertEquals(assistant.id, restored.id)
        assertEquals("Preparar entrevista", restored.savedStateHandle.get<String>("draft"))
        assertSame(session, ViewModelProvider(restored)[SessionState::class.java])
    }

    @Test
    fun reselectingAssistantDoesNotCreateAnotherScreen() {
        navigation.openMainDestination(ASSISTANT_ROUTE)
        val original = navigation.currentBackStackEntry!!.id
        repeat(3) { navigation.openMainDestination(ASSISTANT_ROUTE) }
        assertEquals(original, navigation.currentBackStackEntry!!.id)
        navigation.openMainDestination(AGENDA_ROUTE)
        assertNull(navigation.previousBackStackEntry)
    }

    @Test
    fun editingTasksSurvivesLeavingAndReturningToAssistant() {
        navigation.openAssistant("chat/12,34")
        val editing = navigation.currentBackStackEntry!!
        val session = ViewModelProvider(editing)[SessionState::class.java]
        navigation.openMainDestination(AGENDA_ROUTE)
        assertEquals(AGENDA_ROUTE, navigation.currentDestination?.route)
        navigation.openMainDestination(ASSISTANT_ROUTE)
        val restored = navigation.currentBackStackEntry!!
        assertEquals("12,34", restored.arguments?.getString("taskId"))
        assertEquals(editing.id, restored.id)
        assertSame(session, ViewModelProvider(restored)[SessionState::class.java])
    }

    @Test
    fun plusStartsANewSessionInsteadOfRestoringTheOldDraft() {
        navigation.openMainDestination(ASSISTANT_ROUTE)
        val previous = navigation.currentBackStackEntry!!
        previous.savedStateHandle["draft"] = "Borrador anterior"
        navigation.openMainDestination(AGENDA_ROUTE)
        navigation.openAssistant("chat?initSession=true")
        val fresh = navigation.currentBackStackEntry!!
        assertNotEquals(previous.id, fresh.id)
        assertEquals(true, fresh.arguments?.getBoolean("initSession"))
        assertNull(fresh.savedStateHandle.get<String>("draft"))

        navigation.openMainDestination(AGENDA_ROUTE)
        assertEquals(AGENDA_ROUTE, navigation.currentDestination?.route)
        navigation.openMainDestination(ASSISTANT_ROUTE)
        assertEquals(fresh.id, navigation.currentBackStackEntry!!.id)
    }

    @Test
    fun plusOnFirstLaunchCanReturnToAgendaAndRestoreTheSameSession() {
        navigation.openAssistant("chat?initSession=true")
        val session = navigation.currentBackStackEntry!!.id

        navigation.openMainDestination(AGENDA_ROUTE)
        assertEquals(AGENDA_ROUTE, navigation.currentDestination?.route)

        navigation.openMainDestination(ASSISTANT_ROUTE)
        assertEquals(session, navigation.currentBackStackEntry!!.id)
    }

    @Test
    fun aNewEditReplacesTheSavedEditContext() {
        navigation.openAssistant("chat/12")
        val previous = navigation.currentBackStackEntry!!.id
        navigation.openMainDestination(AGENDA_ROUTE)
        navigation.openAssistant("chat/34")
        assertNotEquals(previous, navigation.currentBackStackEntry!!.id)
        assertEquals("34", navigation.currentBackStackEntry!!.arguments?.getString("taskId"))
    }

    @Test
    fun settingsIsOutsideTheTabsAndBackReturnsToTheSameAgenda() {
        val agenda = navigation.currentBackStackEntry!!.id
        assertEquals(AGENDA_ROUTE, navigation.currentDestination?.mainDestination())
        navigation.navigate("settings")
        assertNull(navigation.currentDestination?.mainDestination())
        navigation.popBackStack()
        assertEquals(agenda, navigation.currentBackStackEntry!!.id)
    }

    class SessionState : ViewModel()
}
