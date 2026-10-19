package com.ibc.procrastinapp.ui.assistant.elements.messagesview

import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.utils.Logger
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConversationMessageTextTest {
    private val fallback = "Tu plan está disponible en Propuesta. Puedes pedirme cambios aquí."
    private val proposal = """{"propuesta":{"tasks":[{"title":"Practicar tests"}]}}"""

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.e(any(), any()) } returns Unit
    }

    @After
    fun tearDown() = unmockkObject(Logger)

    @Test
    fun jsonOnlyProposalUsesTheProvidedLocalizedFallbackWithoutChangingTheMessage() {
        val message = Message.assistantMessage(proposal)
        assertEquals(fallback, conversationMessageText(message, fallback))
        assertEquals(proposal, message.content)
    }

    @Test
    fun structuredReplyShowsItsCommentary() {
        val json = """{"comentario":"Empieza por la teoría.","propuesta":{"tasks":[]}}"""
        assertEquals("Empieza por la teoría.", conversationMessageText(Message.assistantMessage(json), fallback))
    }

    @Test
    fun fencedReplyPreservesTextOnBothSidesAndCommentaryWithoutShowingJson() {
        val json = """{"comentario":"Estudia por las tardes.","propuesta":{"tasks":[]}}"""
        val message = Message.assistantMessage("Vamos paso a paso.\n```json\n$json\n```\n¿Te encaja?")
        assertEquals(
            "Vamos paso a paso.\n\nEstudia por las tardes.\n\n¿Te encaja?",
            conversationMessageText(message, fallback)
        )
    }

    @Test
    fun fencedJsonOnlyProposalUsesFallback() {
        assertEquals(fallback, conversationMessageText(Message.assistantMessage("```JSON\n$proposal\n```"), fallback))
    }

    @Test
    fun ordinaryAssistantTextIsPreserved() {
        val text = "¿Qué días puedes estudiar?"
        assertEquals(text, conversationMessageText(Message.assistantMessage(text), fallback))
    }

    @Test
    fun userJsonIsPreservedAndOnlyTheExistingAutomaticInstructionIsClipped() {
        val message = Message.userMessage("$proposal\nIMPORTANTE: Recuerda darme la lista en JSON.")
        assertEquals(proposal, conversationMessageText(message, fallback))
    }

    @Test
    fun unrelatedJsonAndInvalidProposalShapeRemainReadableAsOriginallyWritten() {
        listOf("""{"example":42}""", """{"propuesta":null}""", """{"propuesta":{"tasks":"example"}}""")
            .forEach { json ->
                assertEquals(json, conversationMessageText(Message.assistantMessage(json), fallback))
            }
    }

    @Test
    fun malformedJsonDoesNotCrashOrLoseTheOriginalText() {
        val text = "Respuesta pendiente: {invalid json}"
        assertEquals(text, conversationMessageText(Message.assistantMessage(text), fallback))
    }
}
