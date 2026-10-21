package com.ibc.procrastinapp.data.ai

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/** Contrato de las respuestas nuevas de IA; el lector del historial antiguo sigue siendo tolerante. */
object AssistantResponseFormat {
    private val taskTextFields = listOf("title", "deadline", "periodicity", "notes", "notify")
    private val taskFields = taskTextFields.toSet() + setOf("id", "priority", "completed", "subtasks")

    val responseFormat = StructuredResponseFormat(
        jsonSchema = ResponseJsonSchema(
            name = "assistant_tasks_v1",
            schema = objectSchema(
                "comentario" to type("string"),
                "propuesta" to objectSchema("tasks" to taskArray())
            ).apply {
                add("\$defs", JsonObject().apply {
                    add("task", objectSchema(
                        "id" to type("integer"),
                        "title" to type("string"),
                        "deadline" to type("string"),
                        "priority" to type("integer").apply {
                            add("enum", JsonArray().apply { (0..3).forEach { add(it) } })
                        },
                        "periodicity" to type("string"),
                        "notes" to type("string"),
                        "completed" to type("boolean"),
                        "notify" to type("string"),
                        "subtasks" to taskArray()
                    ))
                })
            }
        )
    )

    /** Valida antes de publicar o guardar el mensaje; nunca convierte un fallo en tasks: []. */
    fun validate(content: String) {
        try {
            val reader = JsonReader(StringReader(content)).apply { strictness = Strictness.STRICT }
            val root = reader.use {
                val parsed = JsonParser.parseReader(it)
                require(it.peek() == JsonToken.END_DOCUMENT)
                parsed
            }
            val response = exactObject(root, setOf("comentario", "propuesta"))
            requireString(response["comentario"])
            val proposal = exactObject(response["propuesta"], setOf("tasks"))
            validateTasks(proposal["tasks"])
        } catch (_: IllegalArgumentException) {
            throw AIServiceError.InvalidStructuredResponse
        } catch (_: com.google.gson.JsonParseException) {
            throw AIServiceError.InvalidStructuredResponse
        } catch (_: java.io.IOException) {
            throw AIServiceError.InvalidStructuredResponse
        } catch (_: ArithmeticException) {
            throw AIServiceError.InvalidStructuredResponse
        }
    }

    private fun validateTasks(element: JsonElement) {
        require(element.isJsonArray)
        element.asJsonArray.forEach { item ->
            val task = exactObject(item, taskFields)
            taskTextFields.forEach { requireString(task[it]) }
            require(task["title"].asString.isNotBlank())
            require(integer(task["id"]) >= 0)
            require(integer(task["priority"]) in 0L..3L)
            require(task["completed"].isJsonPrimitive && task["completed"].asJsonPrimitive.isBoolean)
            validateTasks(task["subtasks"])
        }
    }

    private fun exactObject(element: JsonElement, fields: Set<String>): JsonObject {
        require(element.isJsonObject)
        return element.asJsonObject.also { require(it.keySet() == fields) }
    }

    private fun requireString(element: JsonElement) {
        require(element.isJsonPrimitive && element.asJsonPrimitive.isString)
    }

    private fun integer(element: JsonElement): Long {
        require(element.isJsonPrimitive && element.asJsonPrimitive.isNumber)
        return element.asBigDecimal.longValueExact()
    }

    private fun type(name: String) = JsonObject().apply { addProperty("type", name) }

    private fun taskArray() = type("array").apply {
        add("items", JsonObject().apply { addProperty("\$ref", "#/\$defs/task") })
    }

    private fun objectSchema(vararg properties: Pair<String, JsonObject>) = type("object").apply {
        add("properties", JsonObject().apply { properties.forEach { (name, schema) -> add(name, schema) } })
        add("required", JsonArray().apply { properties.forEach { (name, _) -> add(name) } })
        addProperty("additionalProperties", false)
    }
}
