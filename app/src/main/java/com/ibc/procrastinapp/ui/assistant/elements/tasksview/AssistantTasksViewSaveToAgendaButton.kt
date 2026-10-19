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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R

/**
 * Botón para guardar tareas en la agenda
 *
 * @param onClick Acción al pulsar el botón
 * @param isSaving Si la propuesta se está guardando
 */
@Composable
fun AssistantTasksViewSaveToAgendaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSaving)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.tertiary
            //else
                //MaterialTheme.colorScheme.secondary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        enabled = !isSaving
    ) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = androidx.compose.ui.res.stringResource(id = R.string.assistant_save_to_agenda_cd),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isSaving)
                androidx.compose.ui.res.stringResource(id = R.string.assistant_saving_label)
            else
                androidx.compose.ui.res.stringResource(id = R.string.assistant_save_to_agenda_label),
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
        isSaving = false,
    )
}

@Preview(name = "Clicked - No edit mode")
@Composable
fun ClickNoeditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isSaving = true,
    )
}

@Preview(name = "No clicked - Edit mode")
@Composable
fun NoClickEditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isSaving = false,
    )
}

@Preview(name = "Clicked - Edit mode")
@Composable
fun ClickEditPreview() {
    AssistantTasksViewSaveToAgendaButton(
        onClick = {},
        isSaving = true,
    )
}
