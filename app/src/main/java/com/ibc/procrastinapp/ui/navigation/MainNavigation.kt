package com.ibc.procrastinapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation

const val AGENDA_ROUTE = "task_list"
const val ASSISTANT_ROUTE = "assistant"

fun NavDestination.mainDestination(): String? = hierarchy.firstOrNull {
    it.route == AGENDA_ROUTE || it.route == ASSISTANT_ROUTE
}?.route

fun NavController.openMainDestination(route: String) {
    if (currentDestination?.mainDestination() == route) return
    navigate(route) {
        popUpTo(AGENDA_ROUTE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// The + button and editing are explicit requests, so they replace a saved assistant.
fun NavController.openAssistant(route: String) {
    clearBackStack(ASSISTANT_ROUTE)
    navigate(route) {
        popUpTo(AGENDA_ROUTE)
        launchSingleTop = true
    }
}

fun NavGraphBuilder.assistantDestinations(
    content: @Composable (taskIds: List<Long>, initSession: Boolean) -> Unit
) {
    navigation(startDestination = "chat", route = ASSISTANT_ROUTE) {
        composable("chat") { content(emptyList(), false) }
        composable(
            route = "chat?initSession={initSession}",
            arguments = listOf(navArgument("initSession") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { entry ->
            content(emptyList(), entry.arguments?.getBoolean("initSession") == true)
        }
        composable(
            route = "chat/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            content(entry.arguments?.getString("taskId")?.toTaskIdList().orEmpty(), false)
        }
    }
}
