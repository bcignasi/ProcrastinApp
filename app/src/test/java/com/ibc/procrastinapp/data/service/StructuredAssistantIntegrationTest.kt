package com.ibc.procrastinapp.data.service

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.ai.AIServiceError
import com.ibc.procrastinapp.data.ai.ChatRequest
import com.ibc.procrastinapp.data.ai.ChatResponse
import com.ibc.procrastinapp.data.ai.Choice
import com.ibc.procrastinapp.data.ai.GoalPlanningPrompt
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.utils.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StructuredAssistantIntegrationTest {
    private val aiService = mockk<AIService>()
    private val storage = mockk<MessageStorage>()
    private var savedMessages = emptyList<Message>()

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit
        coEvery { storage.loadMessages() } returns null
        coEvery { storage.saveMessages(any()) } coAnswers {
            savedMessages = firstArg<List<Message>>().toList()
        }
    }

    @After
    fun tearDown() = unmockkObject(Logger)

    @Test
    fun interviewSendsExactlyTheUserTextWithoutAnInstructionToGenerateTasks() = runTest {
        val captured = slot<ChatRequest>()
        coEvery { aiService.sendMessage(capture(captured)) } returns response(
            """{"comentario":"¿Qué quieres aprender?","propuesta":{"tasks":[]}}"""
        )
        val service = ChatAIService(storage, aiService, backgroundScope)
        runCurrent()

        service.sendMessage("  Quiero hacer música  ")

        assertEquals("Quiero hacer música", captured.captured.messages.last { it.isUser }.content)
        assertEquals("Quiero hacer música", service.messages.value.first().content)
    }

    @Test
    fun startupPreservesStoredConversationAndReplacesObsoleteSystemInstructions() = runTest {
        val previous = listOf(
            Message.systemMessage("Instrucciones antiguas"),
            Message.userMessage("Quiero hacer música"),
            Message.assistantMessage("¿Con qué instrumento?"),
            Message.systemMessage("Fecha antigua")
        )
        savedMessages = previous
        coEvery { storage.loadMessages() } coAnswers { savedMessages.toList() }
        val captured = slot<ChatRequest>()
        coEvery { aiService.sendMessage(capture(captured)) } returns response(
            """{"comentario":"¿Qué estilo?","propuesta":{"tasks":[]}}"""
        )
        val service = ChatAIService(storage, aiService, backgroundScope)
        runCurrent()

        assertEquals(previous.filter { !it.isSystem }, service.messages.value)
        assertEquals(previous, savedMessages)
        service.sendMessage("Piano")

        val sent = captured.captured.messages
        assertEquals(previous.filter { !it.isSystem } + Message.userMessage("Piano"), sent.filter { !it.isSystem })
        assertFalse(sent.any { it.content == "Instrucciones antiguas" || it.content == "Fecha antigua" })
        assertTrue(sent.first().isSystem)
        assertEquals(GoalPlanningPrompt.text, sent.first().content)
    }

    @Test
    fun openingATaskWhileLoadingPreservesHistoryAndTheEditingContext() = runTest {
        val loaded = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { loaded.await() }
        val service = ChatAIService(storage, aiService, backgroundScope)
        runCurrent()
        val json = """{"comentario":"Plan","propuesta":{"tasks":[]}}"""
        service.addUserAndAssistantMessage("Tarea para editar", json)
        loaded.complete(listOf(Message.userMessage("Conversación anterior")))
        runCurrent()

        assertEquals(listOf("Conversación anterior", "Tarea para editar", json),
            service.messages.value.map { it.content })
        assertEquals(service.messages.value, savedMessages.filter { !it.isSystem })
    }

    @Test
    fun sendingWhileHistoryLoadsWaitsAndKeepsThePreviousConversation() = runTest {
        val loaded = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { loaded.await() }
        val captured = slot<ChatRequest>()
        coEvery { aiService.sendMessage(capture(captured)) } returns response(
            """{"comentario":"Continuamos","propuesta":{"tasks":[]}}"""
        )
        val service = ChatAIService(storage, aiService, backgroundScope)
        runCurrent()
        val send = launch { service.sendMessage("Piano") }
        runCurrent()
        assertFalse(captured.isCaptured)

        loaded.complete(listOf(Message.userMessage("Quiero hacer música")))
        send.join()

        assertEquals(listOf("Quiero hacer música", "Piano"),
            captured.captured.messages.filter { it.isUser }.map { it.content })
    }

    @Test
    fun newSessionDuringLoadingIsNotReplacedByOldHistory() = runTest {
        val loaded = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { loaded.await() }
        val service = ChatAIService(storage, aiService, backgroundScope)
        runCurrent()
        service.initSession()
        loaded.complete(listOf(Message.userMessage("Conversación anterior")))
        runCurrent()

        assertTrue(service.messages.value.isEmpty())
        assertTrue(savedMessages.none { it.isUser || it.isAssistant })
    }

    @Test
    fun assistantRequestsStrictJsonSchemaAndAcceptsAnEmptyProposalWithCommentary() = runTest {
        val captured = slot<ChatRequest>()
        val reply = """{"comentario":"¿Cuándo quieres aprobar?","propuesta":{"tasks":[]}}"""
        coEvery { aiService.sendMessage(capture(captured)) } returns response(reply)
        val service = ChatAIService(storage, aiService, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        runCurrent()

        repository.sendMessage("Quiero aprobar el teórico")
        runCurrent()

        val request = JsonParser.parseString(Gson().toJson(captured.captured)).asJsonObject
        assertTrue("El Asistente debe pedir un contrato estructurado", request.has("response_format"))
        val format = request.getAsJsonObject("response_format")
        assertEquals("json_schema", format["type"].asString)
        assertTrue(format.getAsJsonObject("json_schema")["strict"].asBoolean)
        assertEquals("gpt-6-luna", captured.captured.model)
        assertEquals(reply, service.messages.value.last().content)
        assertTrue(repository.tasks.value.isEmpty())
        assertNull(repository.error.value)
    }

    @Test
    fun invalidReplyNeverReplacesTheLastValidProposalOrEntersStoredHistory() = runTest {
        val service = ChatAIService(storage, aiService, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        runCurrent()
        // Un contexto antiguo sigue siendo legible aunque no tenga todos los campos nuevos.
        val previous = """{"propuesta":{"tasks":[{"title":"Aprobar el teórico","subtasks":[]}]}}"""
        repository.addUserAndAssistantMessage("Tarea para editar", previous)
        runCurrent()
        val savedBeforeRequest = savedMessages
        val malformed = """{"comentario":"Nuevo plan","propuesta":{"tasks":[{"title":null}]}}"""
        coEvery { aiService.sendMessage(any()) } returns response(malformed)

        repository.sendMessage("Adapta el plan a mis tardes")
        runCurrent()

        assertSame(AIServiceError.InvalidStructuredResponse, service.error.value)
        assertEquals(listOf("Aprobar el teórico"), repository.tasks.value.map { it.title })
        assertEquals(previous, service.messages.value.last { it.isAssistant }.content)
        assertFalse(service.messages.value.any { it.content == malformed })
        assertEquals(savedBeforeRequest, savedMessages)
        assertFalse(service.isLoading.value)
    }

    @Test
    fun incompleteAndRefusedRepliesKeepTheProposalAndAllowASuccessfulRetry() = runTest {
        val service = ChatAIService(storage, aiService, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        runCurrent()
        val previous = """{"propuesta":{"tasks":[{"title":"Plan anterior"}]}}"""
        repository.addUserAndAssistantMessage("Tarea para editar", previous)
        runCurrent()

        for (error in listOf(AIServiceError.IncompleteResponse, AIServiceError.RefusedResponse)) {
            coEvery { aiService.sendMessage(any()) } throws error
            repository.sendMessage("Ajusta el plan")
            runCurrent()
            assertSame(error, repository.error.value)
            assertEquals("Plan anterior", repository.tasks.value.single().title)
            assertEquals(previous, service.messages.value.last { it.isAssistant }.content)
            assertEquals(previous, savedMessages.last { it.isAssistant }.content)
            assertFalse(service.isLoading.value)
        }

        val valid = """{"comentario":"Plan ajustado","propuesta":{"tasks":[{"id":0,"title":"Aprobar el teórico","deadline":"","priority":1,"periodicity":"","notes":"","completed":false,"notify":"","subtasks":[]}]}}"""
        coEvery { aiService.sendMessage(any()) } returns response(valid)
        repository.sendMessage("Vuelve a intentarlo")
        runCurrent()
        assertNull(repository.error.value)
        assertEquals("Aprobar el teórico", repository.tasks.value.single().title)
        assertEquals(valid, savedMessages.last().content)
    }

    @Test
    fun cancellationDoesNotBecomeAVisibleErrorOrLeaveLoadingActive() = runTest {
        val service = ChatAIService(storage, aiService, backgroundScope)
        runCurrent()
        coEvery { aiService.sendMessage(any()) } throws CancellationException("Cancelado")
        try {
            service.sendMessage("Mi objetivo")
            fail("Debe propagar la cancelación")
        } catch (_: CancellationException) {
            assertNull(service.error.value)
            assertFalse(service.isLoading.value)
            assertTrue(service.messages.value.none { it.isAssistant })
        }
    }

    private fun response(content: String) = ChatResponse(listOf(Choice(Message.assistantMessage(content))))
}
