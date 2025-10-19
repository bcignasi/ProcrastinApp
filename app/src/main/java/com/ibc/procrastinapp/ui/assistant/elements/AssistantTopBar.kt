/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
    onGoToTaskList: () -> Unit,
    //onToggleViewMode: () -> Unit,
    onDeleteAll: () -> Unit,
    currentScreenMode: AssistantScreenType,
    onCurrentScreenModeChange: (AssistantScreenType) -> Unit,
) {
    TopAppBar(

        /*
                SUPRIMO LAS FLECHAS DE NAVEGACIÓN ESTÁNDAR A LA IZQUIERDA DE LA TOOLBAR
                Solo hay dos pantallas, y el icono de la derecha ya se hace cargo de la navegación
        //        // Icono de flecha para volver a la lista de tareas
        //        navigationIcon = {
        //            IconButton(onClick = onGoToTaskList) {
        //                Icon(
        //                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        //                    contentDescription = "Volver a TaskList"
        //                )
        //            }
        //        },
        */

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

        // Iconos de botones de acción: Info y Delete
        actions = {
            // Botón para borrar todos los mensajes
            IconButton(onClick = onDeleteAll) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.assistant_delete_all_cd),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 🎯 Ir a TaskList
            IconButton(onClick = onGoToTaskList) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = androidx.compose.ui.res.stringResource(id = R.string.assistant_go_to_tasklist_cd))
            }
        },
    )
}