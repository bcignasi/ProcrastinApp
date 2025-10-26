/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.ai

/**
 * Módulo que define las estructuras de datos para la comunicación con servicios de chat IA.
 *
 * Proporciona clases para representar solicitudes ([ChatRequest]), respuestas ([ChatResponse])
 * Los mensajes individuales ([Message]) para la conversación con modelos de lenguaje
 * (Message va en módulo aparte.
 */

/**
 * Solicitud enviada al servidor de IA.
 *
 * @property model Identificador del modelo a utilizar.
 * @property messages Lista de mensajes de la conversación.
 */
data class ChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>
)

/**
 * Respuesta recibida del servidor de IA.
 *
 * @property choices Lista de mensajes de respuesta generados.
 */
data class ChatResponse(
    val choices: List<Choice>
)

/**
 * Opción individual de respuesta del modelo de IA.
 *
 * @property message El mensaje generado.
 */
data class Choice(
    val message: Message
)

