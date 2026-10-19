package com.ibc.procrastinapp.ui.assistant.elements.messagesview

import com.google.gson.JsonParser
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.service.AssistantResponseParserError
import com.ibc.procrastinapp.data.service.AssistantResponseParserImpl
import com.ibc.procrastinapp.ui.common.clipAutomaticText

/** Texto de la burbuja; nunca modifica el mensaje usado por la sesión o por Propuesta. */
internal fun conversationMessageText(message: Message, proposalFallback: String): String {
    val originalText = clipAutomaticText(message.content)
    if (!message.isAssistant) return originalText

    val response = try {
        AssistantResponseParserImpl().parse(message.content)
    } catch (_: AssistantResponseParserError) {
        // Un contenido que no reconocemos debe seguir disponible, sin romper la conversación.
        return originalText
    }
    if (!response.hasJson) return originalText

    // El parser admite cualquier objeto JSON; solo ocultamos el formato de propuestas de tareas.
    val proposal = JsonParser.parseString(response.json).asJsonObject.get("propuesta")
    if (proposal?.isJsonObject != true || proposal.asJsonObject.get("tasks")?.isJsonArray != true) {
        return originalText
    }

    // Conservamos también el texto posterior al JSON, que el parser compartido no devuelve.
    val jsonStart = message.content.indexOf(response.json)
    val before = message.content.substring(0, jsonStart)
        .replace(Regex("```(?:json)?\\s*$", RegexOption.IGNORE_CASE), "").trim()
    val after = message.content.substring(jsonStart + response.json.length)
        .trimStart().removePrefix("```").trim()

    return listOf(clipAutomaticText(before), response.commentary, clipAutomaticText(after))
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n\n")
        .ifBlank { proposalFallback }
}
