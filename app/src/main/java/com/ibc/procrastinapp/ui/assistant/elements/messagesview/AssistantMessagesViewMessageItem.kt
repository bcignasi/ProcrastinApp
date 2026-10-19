/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.messagesview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.Message

/**
 * Componente que representa un mensaje individual en la conversación
 * @param message el mensaje que se va a mostrar
 */
@Composable
fun AssistantMessagesViewMessageItem(message: Message) {
    val isUser = message.isUser
    val author = stringResource(if (isUser) R.string.avatar_user else R.string.avatar_assistant)

    val proposalFallback = stringResource(R.string.assistant_proposal_ready)
    val displayText = remember(message, proposalFallback) {
        conversationMessageText(message, proposalFallback)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Avatar para el asistente
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = if (isUser) Modifier.fillMaxWidth(0.85f) else Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Burbuja de mensaje
            Surface(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = author
                },
                shape = MaterialTheme.shapes.large,
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = displayText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

    }
}

//--------------------------------------------------------------------
@Preview
@Composable
fun PreviewMessageItem() {
    Column {
        AssistantMessagesViewMessageItem(
            message = Message.Companion.assistantMessage(content = "Hola, ¿en qué puedo ayudarte?")
        )
        AssistantMessagesViewMessageItem(
            message = Message.Companion.userMessage(content = "Necesito planificar una tarea.")
        )
        AssistantMessagesViewMessageItem(
            message = Message.Companion.assistantMessage(content = "Te he entendido")
        )
    }
}
