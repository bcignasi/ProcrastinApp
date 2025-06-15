/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// SelectionToolbar.kt
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Barra de herramientas que se muestra cuando hay tareas seleccionadas
 *
 * @param selectionCount Número de tareas seleccionadas
 * @param onActionsClick Acción al pulsar el botón de acciones
 */
@Composable
fun SelectionToolbar(
    selectionCount: Int,
    onActionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$selectionCount seleccionadas",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = onActionsClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Acciones")
        }
    }
}