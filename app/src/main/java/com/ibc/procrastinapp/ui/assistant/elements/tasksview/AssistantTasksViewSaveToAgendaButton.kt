/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Botón para guardar tareas en la agenda
 *
 * @param onClick Acción al pulsar el botón
 * @param isClicked Si el botón está en estado pulsado
 */
@Composable
fun AssistantTasksViewSaveToAgendaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isClicked: Boolean = false,
) {
    Button(
        onClick = {
            if (!isClicked) {
                onClick()
            }
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isClicked)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.tertiary
            //else
                //MaterialTheme.colorScheme.secondary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        enabled = !isClicked // Deshabilitar el botón cuando está en estado "clicked"
    ) {
        Icon(
            imageVector = if (isClicked)
                Icons.Default.Check
            else
                Icons.Default.Save,
            contentDescription = "Send to Agenda",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isClicked)
                "Done"
            else
                "Save to Agenda",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

//-----------------------------------------------------------------------------

@Preview(name = "No clicked - No edit mode")
@Composable
fun NoClickNoEditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isClicked = false,
    )
}

@Preview(name = "Clicked - No edit mode")
@Composable
fun ClickNoeditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isClicked = true,
    )
}

@Preview(name = "No clicked - Edit mode")
@Composable
fun NoClickEditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isClicked = false,
    )
}

@Preview(name = "Clicked - Edit mode")
@Composable
fun ClickEditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isClicked = true,
    )
}