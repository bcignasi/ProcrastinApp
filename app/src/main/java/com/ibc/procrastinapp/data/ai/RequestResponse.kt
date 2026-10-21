/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.ai

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

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
 * @property responseFormat Contrato opcional; las peticiones de frases siguen usando texto libre.
 */
data class ChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>,
    @SerializedName("response_format")
    val responseFormat: StructuredResponseFormat? = null
)

data class StructuredResponseFormat(
    val type: String = "json_schema",
    @SerializedName("json_schema") val jsonSchema: ResponseJsonSchema
)

data class ResponseJsonSchema(
    val name: String,
    val strict: Boolean = true,
    val schema: JsonObject
)

// Datos de transporte: la API puede devolver contenido nulo o una denegación.
// Se comprueban antes de construir los Message no nulos que persiste la app.
data class ChatCompletionApiResponse(val choices: List<ChatCompletionApiChoice?>?)

data class ChatCompletionApiChoice(
    val message: ChatCompletionApiMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChatCompletionApiMessage(
    val role: String?,
    val content: String?,
    val refusal: String?
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

