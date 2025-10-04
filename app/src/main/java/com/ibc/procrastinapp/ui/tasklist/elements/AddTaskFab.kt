/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ibc.procrastinapp.R

/**
 * Botón de acción flotante para añadir tareas
 *
 * @param onClick Acción al pulsar el botón
 */
@Composable
fun AddTaskFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = androidx.compose.ui.res.stringResource(id = R.string.addtaskfab_create_new_task_cd)
        )
    }
}