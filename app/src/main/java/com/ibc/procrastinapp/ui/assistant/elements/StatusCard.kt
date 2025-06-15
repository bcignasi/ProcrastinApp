/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.ui.assistant.AssistantState

/**
 * Componente para mostrar mensajes de error o éxito
 */
@Composable
fun StatusCard(
    viewModelInfo: AssistantState.ViewModelInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (viewModelInfo) {
        is AssistantState.ViewModelInfo.Success -> MaterialTheme.colorScheme.primaryContainer
        is AssistantState.ViewModelInfo.Warning -> MaterialTheme.colorScheme.secondaryContainer
        is AssistantState.ViewModelInfo.Error -> MaterialTheme.colorScheme.errorContainer
    }

    val textColor = when (viewModelInfo) {
        is AssistantState.ViewModelInfo.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        is AssistantState.ViewModelInfo.Warning -> MaterialTheme.colorScheme.onSecondaryContainer
        is AssistantState.ViewModelInfo.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            contentColor = textColor,
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (viewModelInfo) {
                    is AssistantState.ViewModelInfo.Success -> Icons.Default.CheckCircle
                    is AssistantState.ViewModelInfo.Warning -> Icons.Default.Warning
                    is AssistantState.ViewModelInfo.Error -> Icons.Default.Error
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = viewModelInfo.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // 🎯 Botón de cierre manual (una X)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar mensaje")
            }

        }
    }
}

///////////////////////////////////////////////////////////////////////////

@Preview(showBackground = true, name = "StatusCard Success")
@Composable
fun StatusCardPreview() {
    StatusCard(
        viewModelInfo = AssistantState.ViewModelInfo.Success(
            "Mensaje de prueba de confirmación"
        ),
        onDismiss = {} // ✅ callback vacío para preview
    )
}

@Preview(showBackground = true, name = "StatusCard Warning")
@Composable
fun StatusCardPreview2() {
    StatusCard(
        viewModelInfo = AssistantState.ViewModelInfo.Warning(
            "Mensaje de prueba de aviso"
        ),
        onDismiss = {} // ✅ callback vacío para preview
    )
}

@Preview(showBackground = true, name = "StatusCard Error")
@Composable
fun StatusCardPreview3() {
    StatusCard(
        viewModelInfo = AssistantState.ViewModelInfo.Error(
            "Mensaje de prueba de error"
        ),
        onDismiss = {} // ✅ callback vacío para preview
    )
}