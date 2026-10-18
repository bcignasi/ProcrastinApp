/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.ibc.procrastinapp.ui.assistant.AssistantScreenType
import com.ibc.procrastinapp.ui.common.TitleDropdownMenu
import com.ibc.procrastinapp.R

/**
 * Barra superior de la aplicación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantTopBar(
    onDeleteAll: () -> Unit,
    currentScreenMode: AssistantScreenType,
    onCurrentScreenModeChange: (AssistantScreenType) -> Unit,
) {
    TopAppBar(

        title = {
            TitleDropdownMenu(
                currentType = currentScreenMode,
                allTypes = AssistantScreenType.entries,
                onTypeSelected = onCurrentScreenModeChange,
            )
        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),

        // La navegación entre pantallas está en la barra inferior.
        actions = {
            // Botón para borrar todos los mensajes
            IconButton(onClick = onDeleteAll) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.assistant_delete_all_cd),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

        },
    )
}
