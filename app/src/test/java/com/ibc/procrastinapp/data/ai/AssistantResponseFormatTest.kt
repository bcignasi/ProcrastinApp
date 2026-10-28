package com.ibc.procrastinapp.data.ai

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ibc.procrastinapp.data.model.Task
import org.junit.Assert.*
import org.junit.Test

class AssistantResponseFormatTest {
    private val gson = Gson()

    @Test
    fun serializedRequestUsesTheApiFieldNamesAndAClosedRecursiveSchema() {
        val request = ChatRequest(
            messages = listOf(Message.userMessage("Objetivo")),
            responseFormat = AssistantResponseFormat.responseFormat
        )
        val json = gson.toJsonTree(request).asJsonObject
        assertFalse(json.has("responseFormat"))
        val format = json.getAsJsonObject("response_format")
        assertEquals("json_schema", format["type"].asString)
        assertFalse(format.has("jsonSchema"))
        val contract = format.getAsJsonObject("json_schema")
        assertEquals("assistant_tasks_v2", contract["name"].asString)
        assertTrue(contract["strict"].asBoolean)
        val schema = contract.getAsJsonObject("schema")
        assertClosedObjects(schema)
        assertEquals(setOf("comentario", "propuesta"), schema.getAsJsonObject("properties").keySet())
        val task = schema.getAsJsonObject("\$defs").getAsJsonObject("task")
        assertEquals(gson.toJsonTree(Task(title = "Paso")).asJsonObject.keySet(), task.getAsJsonObject("properties").keySet())
        val taskProperties = task.getAsJsonObject("properties")
        assertEquals(listOf(0, 1, 2, 3), taskProperties.getAsJsonObject("priority").getAsJsonArray("enum").map { it.asInt })
        val tasks = schema.getAsJsonObject("properties").getAsJsonObject("propuesta")
            .getAsJsonObject("properties").getAsJsonObject("tasks")
        assertEquals("#/\$defs/task", tasks.getAsJsonObject("items")["\$ref"].asString)
        assertEquals("#/\$defs/task", taskProperties.getAsJsonObject("subtasks").getAsJsonObject("items")["\$ref"].asString)
    }

    @Test
    fun ordinaryRequestsSuchAsQuotesDoNotSendAResponseFormat() {
        val json = gson.toJsonTree(ChatRequest(messages = listOf(Message.userMessage("Una frase")))).asJsonObject
        assertFalse(json.has("response_format"))
        assertEquals("gpt-4o-mini", json["model"].asString)
    }

    @Test
    fun acceptsNestedTasksAndPreservesTheirOriginalJsonAndIds() {
        val task = Task(id = 42, title = "Teórico", subtasks = listOf(
            Task(id = 43, title = "Estudiar", subtasks = listOf(Task(title = "Elegir manual")))
        ))
        val response = reply(task)
        val original = response.toString()
        AssistantResponseFormat.validate(original)
        assertEquals(original, response.toString())
    }

    @Test
    fun acceptsACommentaryWithNoTasksDuringConversation() {
        AssistantResponseFormat.validate("""{"comentario":"¿Qué plazo prefieres?","propuesta":{"tasks":[]}}""")
    }

    @Test
    fun rejectsMissingUnexpectedAndNullFieldsAtEveryLevel() {
        rejects(reply().apply { remove("comentario") })
        rejects(reply().apply { addProperty("extra", true) })
        rejects(reply().apply { getAsJsonObject("propuesta").addProperty("extra", true) })
        for (field in gson.toJsonTree(Task(title = "Paso")).asJsonObject.keySet()) {
            rejects(reply().apply { task(this).remove(field) })
            rejects(reply().apply { task(this).add(field, com.google.gson.JsonNull.INSTANCE) })
        }
        rejects(reply().apply { task(this).addProperty("extra", true) })
        rejects(reply().apply { add("comentario", com.google.gson.JsonNull.INSTANCE) })
    }

    @Test
    fun rejectsCoercedTypesInvalidNumbersAndBlankTitles() {
        rejects(reply().apply { task(this).addProperty("title", 123) })
        rejects(reply().apply { task(this).addProperty("title", "  ") })
        rejects(reply().apply { task(this).addProperty("completed", "false") })
        rejects(reply().apply { task(this).addProperty("priority", "2") })
        rejects(reply().apply { task(this).addProperty("priority", 1.5) })
        rejects(reply().apply { task(this).addProperty("priority", 4) })
        rejects(reply().apply { task(this).addProperty("priority", -1) })
        rejects(reply().apply { task(this).addProperty("id", -1) })
        rejects(reply().apply { task(this).addProperty("id", 0.5) })
        rejects(reply().apply { task(this).add("id", JsonParser.parseString("9223372036854775808")) })
        rejects(reply().apply { task(this).add("subtasks", JsonObject()) })
        rejects(reply().apply { getAsJsonObject("propuesta").add("tasks", JsonObject()) })
        rejects(reply(Task(title = "Meta", subtasks = listOf(Task(title = "")))))
    }

    @Test
    fun rejectsMalformedJsonMarkdownProseAndMultipleDocuments() {
        val valid = reply().toString()
        for (content in listOf("", "null", "[]", "{", "```json\n$valid\n```", "Tu plan: $valid", "$valid $valid", "/* comentario */$valid", valid.replace("\"comentario\"", "comentario"))) {
            assertThrows(content, AIServiceError.InvalidStructuredResponse::class.java) {
                AssistantResponseFormat.validate(content)
            }
        }
    }

    @Test
    fun acceptsScheduledDaysWithoutRequiringATimeOrReminder() {
        for (day in listOf("", "2026-11-02", "2028-02-29")) {
            val response = reply().apply { task(this).addProperty("scheduledDate", day) }
            AssistantResponseFormat.validate(response.toString())
        }
    }

    @Test
    fun rejectsInvalidScheduledDaysIncludingInsideSubtasks() {
        for (day in listOf("lunes", "2026-02-30", "2026-02-29", "2026-1-02", "2026-11-02 12:00", " ")) {
            val response = reply(Task(title = "Meta", subtasks = listOf(Task(title = "Sesión"))))
            task(response).getAsJsonArray("subtasks")[0].asJsonObject.addProperty("scheduledDate", day)
            rejects(response)
        }
    }

    private fun reply(task: Task = Task(title = "Paso")): JsonObject = JsonObject().apply {
        addProperty("comentario", "Aquí tienes tu plan")
        add("propuesta", JsonObject().apply { add("tasks", gson.toJsonTree(listOf(task))) })
    }

    private fun task(reply: JsonObject) = reply.getAsJsonObject("propuesta").getAsJsonArray("tasks")[0].asJsonObject

    private fun rejects(reply: JsonObject) {
        assertThrows(reply.toString(), AIServiceError.InvalidStructuredResponse::class.java) {
            AssistantResponseFormat.validate(reply.toString())
        }
    }

    private fun assertClosedObjects(schema: JsonElement) {
        if (schema.isJsonArray) schema.asJsonArray.forEach(::assertClosedObjects)
        if (!schema.isJsonObject) return
        val obj = schema.asJsonObject
        if (obj["type"]?.asString == "object") {
            assertFalse(obj["additionalProperties"].asBoolean)
            assertEquals(obj.getAsJsonObject("properties").keySet(), obj.getAsJsonArray("required").map { it.asString }.toSet())
        }
        obj.entrySet().forEach { assertClosedObjects(it.value) }
    }
}
