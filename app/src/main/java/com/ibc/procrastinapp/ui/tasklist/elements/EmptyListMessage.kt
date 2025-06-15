/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Mensaje cuando no hay tareas disponibles
 */
@Composable
fun EmptyListMessage(modifier: Modifier = Modifier.Companion) {
    Text(
        text = "No hay tareas.\nCrea nuevas tareas con el asistente de IA.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Companion.Center,
        modifier = modifier.padding(16.dp)
    )
}