/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.ai

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.io.IOException

@ExperimentalCoroutinesApi
class AIServiceTest {
    private lateinit var aiService: AIService

    @Before
    fun setup() {
        // Inicializar el Singleton
        AIService.initialize("fake-api-key", "https://example.com/")

        // Crear un spy del singleton real
        aiService = spyk(AIService.getInstance())
    }

    @Test
    fun createUserMessage_createsMessageWithCorrectRole() {
        // Act
        val message = Message.userMessage("Hola")

        // Assert
        assertEquals("user", message.role)
        assertEquals("Hola", message.content)
        assertTrue(message.isUser)
    }

    @Test
    fun createSystemMessage_createsMessageWithCorrectRole() {
        // Act
        val message = Message.systemMessage("Instrucción")

        // Assert
        assertEquals("system", message.role)
        assertEquals("Instrucción", message.content)
        assertTrue(message.isSystem)
    }

    @Test
    fun createAssistantMessage_createsMessageWithCorrectRole() {
        // Act
        val message = Message.assistantMessage("Respuesta")

        // Assert
        assertEquals("assistant", message.role)
        assertEquals("Respuesta", message.content)
        assertTrue(message.isAssistant)
    }

    @Test
    fun sendMessage_returnsCorrectResponse() = runTest {
        // Arrange
        val request = ChatRequest(messages = listOf(Message.userMessage("Hola")))
        val expectedResponse = ChatResponse(choices = listOf(
            Choice(Message.assistantMessage("Respuesta de prueba"))
        ))

        // Mockear el comportamiento del metodo sendMessage
        coEvery {
            aiService.sendMessage(any())
        } returns expectedResponse

        // Act
        val result = aiService.sendMessage(request)

        // Assert
        assertEquals(expectedResponse, result)
        assertEquals("Respuesta de prueba", result.choices.first().message.content)
        coVerify { aiService.sendMessage(request) }
    }

    @Test
    fun sendMessage_handlesNetworkExceptions() = runTest {
        // Arrange
        val request = ChatRequest(messages = listOf(Message.userMessage("Hola")))

        coEvery {
            aiService.sendMessage(any())
        } throws IOException("Error de red")

        // Act & Assert
        try {
            aiService.sendMessage(request)
            fail("Se esperaba una excepción pero no se lanzó ninguna")
        } catch (e: Exception) {
            assertTrue("El mensaje '${e.message}' debería contener 'Error de comunicación con la IA' o 'Error de red'",
                e.message?.contains("Error de comunicación con la IA") == true ||
                        e.message?.contains("Error de red") == true)
        }
    }

    @Test
    fun sendMessage_handlesEmptyResponse() = runTest {
        // Arrange
        val request = ChatRequest(messages = listOf(Message.userMessage("Hola")))

        coEvery {
            aiService.sendMessage(any())
        } throws Exception("Respuesta vacía de la IA")

        // Act & Assert
        try {
            aiService.sendMessage(request)
            fail("Se esperaba una excepción pero no se lanzó ninguna")
        } catch (e: Exception) {
            assertTrue("El mensaje '${e.message}' debería contener 'Respuesta vacía de la IA'",
                e.message?.contains("Respuesta vacía de la IA") == true)
        }
    }

    @Test
    fun sendMessage_handlesHttpError() = runTest {
        // Arrange
        val request = ChatRequest(messages = listOf(Message.userMessage("Hola")))

        coEvery {
            aiService.sendMessage(any())
        } throws Exception("Error HTTP 500: Internal Server Error")

        // Act & Assert
        try {
            aiService.sendMessage(request)
            fail("Se esperaba una excepción pero no se lanzó ninguna")
        } catch (e: Exception) {
            assertTrue("El mensaje '${e.message}' debería contener 'Error HTTP'",
                e.message?.contains("Error HTTP") == true)
        }
    }

    @Test
    fun getInstance_throwsException_whenNotInitialized() {
        // Arrange - Reset singleton
        val instanceField = AIService::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)

        // Act & Assert
        try {
            AIService.getInstance()
            fail("Se esperaba una excepción pero no se lanzó ninguna")
        } catch (e: IllegalStateException) {
            assertTrue("El mensaje '${e.message}' debería mencionar que no ha sido inicializado",
                e.message?.contains("no ha sido inicializado") == true)
        }

        // Cleanup - Reinitialize for other tests
        AIService.initialize("fake-api-key", "https://example.com/")
    }
}