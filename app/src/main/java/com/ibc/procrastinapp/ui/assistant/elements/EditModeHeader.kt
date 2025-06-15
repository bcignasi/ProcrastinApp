/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Cabecera que muestra información sobre la tarea en modo edición
 * Usado en AssistantScreen
 */
@Composable
fun EditModeHeader(
    taskCount: Int,
    taskIdsString: String = "",
)  {
    Card(
        modifier = Modifier
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (taskCount == 1)
                    "Editando una tarea seleccionada ($taskIdsString)"
                else
                    "Editando $taskCount tareas seleccionadas ($taskIdsString)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Text(
                text = "Pide al asistente los cambios deseados, pulsa 'Update?' para guardar.",
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

///////////////////////////////////////////////////////////////////////////

@Preview
@Composable
fun EditModeHeader1Preview() {
    EditModeHeader(taskCount = 1)
}

@Preview
@Composable
fun EditModeHeaderNPreview() {
    EditModeHeader(taskCount = 4)
}



