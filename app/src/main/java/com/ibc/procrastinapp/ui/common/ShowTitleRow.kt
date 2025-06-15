/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.ibc.procrastinapp.data.model.Task

@Composable
fun ShowTitleRow(
    task: Task,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        // Contenedor para check y título
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Texto del título con indicador de completado integrado
            Text(
                text = buildAnnotatedString {
                    if (task.completed) {
                        // Opción 1: Usar un emoji o carácter Unicode
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize
                            )
                        ) {
                            append("✓ ")  // Checkmark Unicode
                        }

                        // Alternativa: puedes usar otros símbolos como:
                        // "✓" (check), "✔" (check más pesado), "☑" (checkbox), "🗹" (checkbox con check)
                    }

                    // Añadir el título real
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    ) {
                        append(task.title)
                    }
                },
                modifier = Modifier.weight(1f)
            )        }

    }
}