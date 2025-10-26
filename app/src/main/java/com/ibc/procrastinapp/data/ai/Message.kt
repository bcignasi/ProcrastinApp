/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.ai

import kotlinx.serialization.Serializable

/**
 * Mensaje individual en una conversación.
 *
 * Se debe crear mediante los métodos factory: [Message.userMessage],
 * [Message.systemMessage] o [Message.assistantMessage].
 *
 * @property role Rol del emisor ("user", "assistant" o "system").
 * @property content Contenido del mensaje.
 */
@ConsistentCopyVisibility
@Serializable
data class Message private constructor (
    val role: String,
    val content: String
) {
    /** Indica si el mensaje proviene del usuario. */
    val isUser: Boolean
        get() = role == ROLE_USER

    /** Indica si el mensaje proviene del sistema. */
    val isSystem: Boolean
        get() = role == ROLE_SYSTEM

    /** Indica si el mensaje proviene del asistente. */
    val isAssistant: Boolean
        get() = role == ROLE_ASSISTANT

    companion object {
        internal const val ROLE_USER = "user"
        internal const val ROLE_SYSTEM = "system"
        internal const val ROLE_ASSISTANT = "assistant"
        /** Crea un mensaje de usuario. */
        fun userMessage(content: String): Message {
            return Message(ROLE_USER, content)
        }

        /** Crea un mensaje del sistema. */
        fun systemMessage(content: String): Message {
            return Message(ROLE_SYSTEM, content)
        }

        /** Crea un mensaje del asistente. */
        fun assistantMessage(content: String): Message {
            return Message(ROLE_ASSISTANT, content)
        }
    }
}