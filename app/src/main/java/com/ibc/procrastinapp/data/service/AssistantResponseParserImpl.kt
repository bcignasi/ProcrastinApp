/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.google.gson.JsonParser
import com.ibc.procrastinapp.utils.Logger

/**
 * Implementación de AssistantResponseParser que analiza mensajes
 * del asistente con formato JSON específico.
 */
class AssistantResponseParserImpl : AssistantResponseParser {

    private val logTag = "JsonAssistantResponseParser"

    val jsonPattern = """(\{[\s\S]*\})""".toRegex()

    override fun parse(message: String): AssistantResponse {
        if (message.isBlank()) {
            return AssistantResponse()
        }

        try {
            // Buscamos todos los objetos JSON en el mensaje
            val allMatches = jsonPattern.findAll(message).toList()

            // Si no hay JSON, todo_ es texto
            if (allMatches.isEmpty()) {
                return AssistantResponse(text = message.trim())
            }

            // Verificamos si hay múltiples objetos JSON
            if (allMatches.size > 1) {
                Logger.e(logTag, "Se encontraron múltiples objetos JSON: ${allMatches.size}")
                throw IllegalArgumentException("Se encontraron múltiples objetos JSON en el mensaje")
            }

            // Extraemos el único JSON y el texto anterior
            val jsonMatch = allMatches.first()
            val jsonContent = jsonMatch.value
            val textBeforeJson = message.substring(0, message.indexOf(jsonContent)).trim()

            // Intentamos extraer el comentario del JSON
            var comment = ""
            try {
                val jsonObject = JsonParser.parseString(jsonContent).asJsonObject
                if (jsonObject.has("comentario")) {
                    comment = jsonObject.get("comentario").asString.trim()
                }
            } catch (e: Exception) {
                Logger.e(logTag, "Error al parsear JSON para comentario: ${e.message}")
                throw IllegalArgumentException("JSON malformado: no se pudo parsear el contenido", e)
            }

            return AssistantResponse(
                text = textBeforeJson,
                json = jsonContent,
                commentary = comment
            )
        } catch (e: Exception) {
            if (e is IllegalArgumentException) {
                throw e // Re-lanzamos las excepciones de validación
            }
            Logger.e(logTag, "Error al parsear respuesta: ${e.message}")
            return AssistantResponse(text = message.trim())
        }
    }
}
