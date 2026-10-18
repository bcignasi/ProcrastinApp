package com.ibc.procrastinapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ibc.procrastinapp.R

@Composable
fun MainBottomBar(selectedRoute: String, onSelect: (String) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(
            selected = selectedRoute == AGENDA_ROUTE,
            onClick = { onSelect(AGENDA_ROUTE) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text(stringResource(R.string.navigation_agenda)) },
            colors = colors
        )
        NavigationBarItem(
            selected = selectedRoute == ASSISTANT_ROUTE,
            onClick = { onSelect(ASSISTANT_ROUTE) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
            label = { Text(stringResource(R.string.navigation_assistant)) },
            colors = colors
        )
    }
}
