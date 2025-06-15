/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.Logger

data class Propuesta(val tasks: List<Task>?)
data class ResultadoAI(val comentario: String?, val propuesta: Propuesta?)

class TaskJsonExtractor(
    private val gson: Gson = Gson()
) {
    /**
     * Recibe el String Message.content y devuelve la parte JSON como List<Task>
     * De hecho, esto parece un mecanismo similar al de AssistantResponseParserImpl,
     * o sea, que sospecho que se superponen
     */
    fun extractTasksFromText(text: String): List<Task> {
        val regex = Regex("""\{[\s\S]*\}""")
        val matches = regex.findAll(text).toList()
        if (matches.size != 1) return emptyList()

        Logger.d("IBC-TaskJsonExtractor", "Texto original: $text")
        Logger.d("IBC-TaskJsonExtractor", "Texto extraído: ${matches[0].value}")

        return try {
            val result = gson.fromJson(matches[0].value, ResultadoAI::class.java)
            Logger.d("IBC-TaskJsonExtractor", "Resultado fromJson: $result")
            val rawTasks = result.propuesta?.tasks ?: return emptyList()
            val tasks = rawTasks.map { it.withNonNullStrings() }
            if (tasks.any { !isValid(it) }) return emptyList()
            else tasks
        } catch (e: JsonSyntaxException) {
            Logger.e("IBC-TaskJsonExtractor", "Error al analizar JSON: ${e.message}")
            emptyList()
        }
    }

    private fun isValid(task: Task): Boolean {
        if (task.title.isBlank()) return false
        return task.subtasks.all { isValid(it) }
    }
}
