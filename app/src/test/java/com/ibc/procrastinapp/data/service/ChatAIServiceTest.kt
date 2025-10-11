/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.ai.AIServiceError
import com.ibc.procrastinapp.data.ai.ChatResponse
import com.ibc.procrastinapp.data.ai.Choice
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.utils.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ChatAIServiceTest {

    private lateinit var mockAIService: AIService
    private lateinit var mockMessageStorage: MessageStorage
    private lateinit var mockResponseParser: AssistantResponseParserImpl

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: CoroutineScope

    private lateinit var chatAIService: ChatAIService

    @Before
    fun setup() {
        // Configura dispatcher de test y scope de corrutinas
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = CoroutineScope(testDispatcher)

        // Logger estático -> se mockea como objeto
        mockkObject(Logger)
        io.mockk.every { Logger.d(any(), any()) } returns Unit
        io.mockk.every { Logger.w(any(), any()) } returns Unit
        io.mockk.every { Logger.e(any(), any()) } returns Unit
        io.mockk.every { Logger.e(any(), any(), any()) } returns Unit
        io.mockk.every { Logger.i(any(), any()) } returns Unit

        // Mocks principales relajados
        // EXPLAIN relaxed: el parámetro relaxed=true permite que MockK devuelva valores por defecto
        // para cualquier función no especificada explícitamente. Así, evitamos errores si no hemos
        // definido un comportamiento concreto para algún metodo del mock.
        mockAIService = mockk(relaxed = true)
        mockMessageStorage = mockk(relaxed = true)
        mockResponseParser = mockk(relaxed = true)

        // Mensajes iniciales nulos
        coEvery { mockMessageStorage.loadMessages() } returns null

        // Inicializamos el servicio con los mocks
        chatAIService = ChatAIService(
            messageStorage = mockMessageStorage,
            aiService = mockAIService,
            coroutineScope = testScope
        )

        // Inyectamos el parser por reflexión
        // EXPLAIN reflexión: accedemos mediante reflexión a una propiedad privada de la clase
        // (en este caso, responseParser) para sustituir su instancia por un mock.
        // Esto es útil en tests cuando no tenemos forma directa de pasar la dependencia desde fuera.
        val responseParserField = ChatAIService::class.java.getDeclaredField("responseParser")
        responseParserField.isAccessible = true
        responseParserField.set(chatAIService, mockResponseParser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Verifica que initSession limpia el estado, añade el mensaje de sistema y guarda el resultado
    @Test
    fun initSession_createsNewConversation() = runTest {
        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(timeout = 5000) {
            mockMessageStorage.saveMessages(match { messages -> messages.size == 1 && messages[0].isSystem })
        }
        assertEquals(emptyList<Message>(), chatAIService.messages.value)
        assertNull(chatAIService.error.value)
        assertNull(chatAIService.lastResponse.value)
    }

    // Simula el envío de un mensaje y la respuesta del asistente
    @Test
    fun sendMessage_addsUserMessageAndProcessesResponse() = runTest {
        val assistantText = """{"comentario":"Hola","propuesta":{"tasks":[{"title":"Tarea"}]}}"""
        val assistantMessage = Message.assistantMessage(assistantText)
        val parsedResponse = AssistantResponse(text = assistantText, json = """{"tasks":[{"title":"Tarea"}]}""", commentary = "Hola")

        coEvery { mockAIService.sendMessage(any()) } returns ChatResponse(listOf(Choice(assistantMessage)))
        io.mockk.every { mockResponseParser.parse(assistantText) } returns parsedResponse

        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        chatAIService.sendMessage("Hola")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, chatAIService.messages.value.size)
        // assertEquals("Hola", chatAIService.messages.value[0].content)
        assertTrue(chatAIService.messages.value[0].content.startsWith("Hola"))
        assertEquals(assistantText, chatAIService.messages.value[1].content)
        assertEquals(parsedResponse, chatAIService.lastResponse.value)
        assertFalse(chatAIService.isLoading.value)
        assertNull(chatAIService.error.value)
    }

    // Test para AIServiceError.EmptyResponse
    @Test
    fun sendMessage_handlesEmptyResponse() = runTest {
        coEvery { mockAIService.sendMessage(any()) } throws AIServiceError.EmptyResponse

        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        chatAIService.sendMessage("Hola")
        testDispatcher.scheduler.advanceUntilIdle()

        val error = chatAIService.error.value
        assertNotNull(error)
        assertTrue(error is AIServiceError.EmptyResponse)

        assertFalse(chatAIService.isLoading.value)
    }

    // Test para AIServiceError.Http
    @Test
    fun sendMessage_handlesHttpError() = runTest {
        val httpError = AIServiceError.Http(500, "Internal Server Error")
        coEvery { mockAIService.sendMessage(any()) } throws httpError

        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        chatAIService.sendMessage("Hola")
        testDispatcher.scheduler.advanceUntilIdle()

        val error = chatAIService.error.value
        assertNotNull(error)
        assertTrue(error is AIServiceError.Http)
        assertEquals(500, (error as AIServiceError.Http).code)
        assertEquals("Internal Server Error", error.body)

        assertFalse(chatAIService.isLoading.value)
    }

    // Test para AIServiceError.Communication
    @Test
    fun sendMessage_handlesCommunicationError() = runTest {
        val commError = AIServiceError.Communication("Network timeout", Exception("Timeout"))
        coEvery { mockAIService.sendMessage(any()) } throws commError

        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        chatAIService.sendMessage("Hola")
        testDispatcher.scheduler.advanceUntilIdle()

        val error = chatAIService.error.value
        assertNotNull(error)
        assertTrue(error is AIServiceError.Communication)
        assertEquals("Network timeout", (error as AIServiceError.Communication).detail)

        assertFalse(chatAIService.isLoading.value)
    }

    // Test para SaveMessagesException (si la creaste)
    @Test
    fun saveMessages_handlesSaveError() = runTest {
        // Mock para que falle el guardado
        coEvery { mockMessageStorage.saveMessages(any()) } throws Exception("Database error")

        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simula un mensaje que gatille saveMessages internamente
        val mockResponse = ChatResponse(
            choices = listOf(
                Choice(
                    message = Message.assistantMessage("Respuesta del asistente")
                )
            )
        )
        coEvery { mockAIService.sendMessage(any()) } returns mockResponse

        chatAIService.sendMessage("Hola")
        testDispatcher.scheduler.advanceUntilIdle()

        val error = chatAIService.error.value!!
        assertNotNull(error)
        assertTrue(error is SaveMessagesException)
        assertEquals("Database error", error.cause?.message)
    }

    // Verifica que sendMessage captura excepciones y actualiza el estado de error
    @Test
    fun sendMessage_handlesError() = runTest {
        coEvery { mockAIService.sendMessage(any()) } throws Exception("Error de comunicación")

        chatAIService.initSession()
        testDispatcher.scheduler.advanceUntilIdle()

        chatAIService.sendMessage("Hola")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica que hay un error
        val error = chatAIService.error.value
        assertNotNull(error)
        assertTrue(error is Exception)
        assertFalse(error is AIServiceError)  // No es un AIServiceError
        assertEquals("Error de comunicación", error!!.message)

        assertFalse(chatAIService.isLoading.value)
        assertEquals(1, chatAIService.messages.value.size)
        //assertEquals("Hola", chatAIService.messages.value[0].content)
        assertTrue(chatAIService.messages.value[0].content.startsWith("Hola"))
    }

    // Verifica que los mensajes almacenados previamente se cargan correctamente
    @Test
    fun loadMessages_loadsExistingMessages() = runTest {
        val messages = listOf(
            Message.systemMessage("Prompt"),
            Message.userMessage("Hola"),
            Message.assistantMessage("¿En qué puedo ayudarte?")
        )
        coEvery { mockMessageStorage.loadMessages() } returns messages

        chatAIService = ChatAIService(mockMessageStorage, mockAIService, testScope)
        testDispatcher.scheduler.advanceUntilIdle()

        val parserField = ChatAIService::class.java.getDeclaredField("responseParser")
        parserField.isAccessible = true
        parserField.set(chatAIService, mockResponseParser)

        assertEquals(2, chatAIService.messages.value.size)
        assertEquals("Hola", chatAIService.messages.value[0].content)
    }

    // Verifica que si no hay mensajes almacenados se inicializa una nueva sesión
    @Test
    fun loadMessages_handlesEmptyStorage() = runTest {
        // Verifica que si no hay mensajes almacenados se inicializa una nueva sesión
        coEvery { mockMessageStorage.loadMessages() } returns null

        chatAIService = ChatAIService(mockMessageStorage, mockAIService, testScope)
        testDispatcher.scheduler.advanceUntilIdle()

        val parserField = ChatAIService::class.java.getDeclaredField("responseParser")
        parserField.isAccessible = true
        parserField.set(chatAIService, mockResponseParser)

        assertEquals(emptyList<Message>(), chatAIService.messages.value)
        coVerify { mockMessageStorage.saveMessages(any()) }
    }

    // Verifica que se pueden insertar manualmente mensajes del usuario y del asistente
    @Test
    fun addUserAndAssistantMessage_addsMessagesToConversation() = runTest {
        // Verifica que se pueden insertar manualmente mensajes del usuario y del asistente
        val userText = "Editar tarea"
        val assistantText = """{"comentario":"Modificando","propuesta":{"tasks":[{"title":"Editada"}]}}"""
        val parsedResponse = AssistantResponse(text = assistantText, json = """{"tasks":[{"title":"Editada"}]}""", commentary = "Modificando")

        io.mockk.every { mockResponseParser.parse(assistantText) } returns parsedResponse

        chatAIService.addUserAndAssistantMessage(userText, assistantText)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, chatAIService.messages.value.size)
        assertEquals(parsedResponse, chatAIService.lastResponse.value)
        assertEquals(userText, chatAIService.messages.value[0].content)
        assertEquals(assistantText, chatAIService.messages.value[1].content)
        coVerify { mockMessageStorage.saveMessages(any()) }
    }

    // Verifica que los mensajes del sistema se excluyen del flujo mostrado en la UI
    @Test
    fun filterUiMessages_excludesSystemMessages() {
        val messages = listOf(
            Message.systemMessage("Sistema"),
            Message.userMessage("Hola"),
            Message.assistantMessage("¿Cómo estás?")
        )

        val method = ChatAIService::class.java.getDeclaredMethod("filterUiMessages", List::class.java)
        method.isAccessible = true

        val result = method.invoke(chatAIService, messages) as List<*>
        assertEquals(2, result.size)
        assertFalse((result[0] as Message).isSystem)
    }
}
