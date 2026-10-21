package com.ibc.procrastinapp.data.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ibc.procrastinapp.utils.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Ejecuta AIService real; solo sustituye el cliente externo, sin llamadas de red. */
class AIServiceResponseTest {
    private lateinit var service: AIService
    private val client = mockk<AIService.AIApiClient>()
    private val request = ChatRequest(
        messages = listOf(Message.userMessage("Plan")),
        responseFormat = AssistantResponseFormat.responseFormat
    )

    @Before
    fun setup() {
        resetSingleton()
        AIService.initialize("fake-test-key", "https://example.com/")
        service = AIService.getInstance()
        AIService::class.java.getDeclaredField("apiClient").apply {
            isAccessible = true
            set(service, client)
        }
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        resetSingleton()
        unmockkObject(Logger)
    }

    @Test
    fun convertsACompleteApiMessageWithoutChangingItsContent() = runTest {
        val content = """{"comentario":"Hola","propuesta":{"tasks":[]}}"""
        coEvery { client.sendMessage(request) } returns Response.success(wire(content))
        assertEquals(Message.assistantMessage(content), service.sendMessage(request).choices.single().message)
    }

    @Test
    fun ordinaryQuoteRequestsStillAcceptPlainText() = runTest {
        val quoteRequest = ChatRequest(messages = listOf(Message.userMessage("Una frase")))
        coEvery { client.sendMessage(quoteRequest) } returns Response.success(wire("Un paso cada día."))
        assertEquals("Un paso cada día.", service.sendMessage(quoteRequest).choices.single().message.content)
    }

    @Test
    fun handlesARefusalBeforeTryingToReadNullContent() = runTest {
        coEvery { client.sendMessage(any()) } returns Response.success(wire(content = null, refusal = "No puedo ayudar."))
        try {
            service.sendMessage(request)
            fail("Debe comunicar una denegación")
        } catch (error: AIServiceError) {
            assertSame(AIServiceError.RefusedResponse, error)
        }
    }

    @Test
    fun rejectsTruncatedMissingAndUnexpectedFinishReasonsEvenWithValidLookingContent() = runTest {
        for (reason in listOf("length", "tool_calls", null)) {
            coEvery { client.sendMessage(any()) } returns Response.success(wire("{}", reason))
            try {
                service.sendMessage(request)
                fail("No debe aceptar finish_reason=$reason")
            } catch (error: AIServiceError) {
                assertSame(AIServiceError.IncompleteResponse, error)
            }
        }
    }

    @Test
    fun reportsContentFilteringAsARefusal() = runTest {
        coEvery { client.sendMessage(any()) } returns Response.success(wire(content = null, finishReason = "content_filter"))
        try {
            service.sendMessage(request)
            fail("Debe comunicar el bloqueo")
        } catch (error: AIServiceError) {
            assertSame(AIServiceError.RefusedResponse, error)
        }
    }

    @Test
    fun rejectsMissingChoicesMessagesAndContent() = runTest {
        val invalidBodies = listOf(
            "{}", """{"choices":[]}""", """{"choices":[null]}""",
            """{"choices":[{"finish_reason":"stop","message":null}]}"""
        ).map { Gson().fromJson(it, ChatCompletionApiResponse::class.java) } +
            listOf(wire(content = null), wire(content = " "))
        for (body in invalidBodies) {
            coEvery { client.sendMessage(any()) } returns Response.success(body)
            try {
                service.sendMessage(request)
                fail("No debe aceptar una respuesta vacía")
            } catch (error: AIServiceError) {
                assertSame(AIServiceError.EmptyResponse, error)
            }
        }
    }

    @Test
    fun rejectsAnUnexpectedRoleBeforeCreatingAPersistedMessage() = runTest {
        coEvery { client.sendMessage(any()) } returns Response.success(wire(role = "user"))
        try {
            service.sendMessage(request)
            fail("Solo se admiten respuestas del asistente")
        } catch (error: AIServiceError) {
            assertSame(AIServiceError.InvalidStructuredResponse, error)
        }
    }

    @Test
    fun preservesHttpErrorsInsteadOfWrappingThemAsCommunicationErrors() = runTest {
        coEvery { client.sendMessage(any()) } returns Response.error(
            400, "Invalid schema".toResponseBody("text/plain".toMediaType())
        )
        try {
            service.sendMessage(request)
            fail("Debe comunicar el error HTTP")
        } catch (error: AIServiceError.Http) {
            assertEquals(400, error.code)
            assertEquals("Invalid schema", error.body)
        }
    }

    @Test
    fun wrapsNetworkFailuresAndKeepsTheirCause() = runTest {
        val networkError = IOException("Sin conexión")
        coEvery { client.sendMessage(any()) } throws networkError
        try {
            service.sendMessage(request)
            fail("Debe comunicar el error de red")
        } catch (error: AIServiceError.Communication) {
            assertSame(networkError, error.cause)
        }
    }

    @Test
    fun cancellationRemainsCancellation() = runTest {
        val cancellation = CancellationException("Cancelado")
        coEvery { client.sendMessage(any()) } throws cancellation
        try {
            service.sendMessage(request)
            fail("Debe propagar la cancelación")
        } catch (error: CancellationException) {
            // La recuperación de stack traces de coroutines puede copiar la excepción.
            assertEquals(cancellation.message, error.message)
        }
    }

    private fun wire(
        content: String? = "Respuesta", finishReason: String? = "stop",
        refusal: String? = null, role: String? = "assistant"
    ): ChatCompletionApiResponse {
        val message = JsonObject().apply {
            addProperty("role", role)
            addProperty("content", content)
            addProperty("refusal", refusal)
        }
        val choice = JsonObject().apply {
            add("message", message)
            addProperty("finish_reason", finishReason)
        }
        val json = """{"choices":[$choice]}"""
        return Gson().fromJson(json, ChatCompletionApiResponse::class.java)
    }

    private fun resetSingleton() {
        AIService::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            set(null, null)
        }
    }
}
