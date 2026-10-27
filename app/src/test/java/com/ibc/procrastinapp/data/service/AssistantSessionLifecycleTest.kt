package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.data.ai.*
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.utils.Logger
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantSessionLifecycleTest {
    private val storage = mockk<MessageStorage>()
    private val ai = mockk<AIService>()
    private var saved = emptyList<Message>()

    @Before fun setup() {
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        coEvery { storage.saveMessages(any()) } coAnswers { saved = firstArg<List<Message>>().toList() }
    }

    @After fun cleanup() = unmockkObject(Logger)

    @Test fun resetWhileInitialLoadIsPendingMustDiscardTheQueuedUserMessage() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        val requests = mutableListOf<ChatRequest>()
        coEvery { ai.sendMessage(capture(requests)) } returns ChatResponse(listOf(
            Choice(Message.assistantMessage("""{"comentario":"Respuesta anterior","propuesta":{"tasks":[]}}"""))
        ))
        val service = ChatAIService(storage, ai, backgroundScope)
        runCurrent()
        val send = launch { service.sendMessage("Mi objetivo anterior") }
        runCurrent()
        assertTrue(requests.isEmpty())
        service.initSession()
        history.complete(emptyList())
        send.join()
        runCurrent()

        assertEquals("Reiniciar debe descartar el envío que aún no había llegado a la API", 0, requests.size)
        assertTrue(service.messages.value.isEmpty())
    }

    @Test fun cancellingAnEditDuringInitialLoadMustNotRestoreTheCancelledProposal() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        val service = ChatAIService(storage, ai, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        runCurrent()
        repository.addUserAndAssistantMessage("Tarea para editar",
            """{"propuesta":{"tasks":[{"id":7,"title":"Edición cancelada","subtasks":[]}]}}""")
        assertEquals("Edición cancelada", repository.tasks.value.single().title)
        // AssistantViewModel.cancelEdit calls clearTasks and clears originalTaskIds.
        repository.clearTasks()
        assertTrue(repository.tasks.value.isEmpty())
        history.complete(emptyList())
        runCurrent()

        assertTrue("La edición cancelada reaparece al acabar la carga", repository.tasks.value.isEmpty())
        assertTrue(service.messages.value.none { it.content.contains("Edición cancelada") })
        assertTrue(saved.none { it.content.contains("Edición cancelada") })
    }

    @Test fun cancellingDuringLoadAlsoSuppressesTheRestoredHistoricalProposal() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        val service = ChatAIService(storage, ai, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        runCurrent()
        repository.addUserAndAssistantMessage("Editar", proposal("Edición cancelada"))
        repository.clearTasks()
        history.complete(listOf(Message.assistantMessage(proposal("Propuesta histórica"))))
        runCurrent()
        assertTrue(repository.tasks.value.isEmpty())
        assertNull(repository.lastResponse.value)
        assertEquals("Propuesta histórica", TaskJsonExtractor().extractTasks(service.messages.value.single().content).single().title)
        coEvery { ai.sendMessage(any()) } returns response("Seguimos")
        repository.sendMessage("Quiero continuar")
        runCurrent()
        assertEquals("Seguimos", repository.lastResponse.value?.commentary)
    }

    @Test fun reopeningAfterCancelDuringLoadPublishesOnlyTheNewEdit() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        val service = ChatAIService(storage, ai, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        runCurrent()
        repository.addUserAndAssistantMessage("Editar A", proposal("A"))
        repository.clearTasks()
        repository.addUserAndAssistantMessage("Editar B", proposal("B"))
        history.complete(emptyList())
        runCurrent()
        assertEquals("B", repository.tasks.value.single().title)
        assertEquals(listOf("Editar B"), service.messages.value.filter { it.isUser }.map { it.content })
    }

    @Test fun waitingForHistoryIsBusyAndDoesNotAcceptASecondSend() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        val reply = CompletableDeferred<ChatResponse>()
        val requests = mutableListOf<ChatRequest>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        coEvery { ai.sendMessage(capture(requests)) } coAnswers { reply.await() }
        val service = ChatAIService(storage, ai, backgroundScope)
        val first = launch { service.sendMessage("Primero") }
        runCurrent()
        val busyDuringLoad = service.isLoading.value
        val second = launch { service.sendMessage("Segundo") }
        runCurrent()
        history.complete(emptyList())
        runCurrent()
        reply.complete(response("Respuesta"))
        first.join()
        second.join()
        assertTrue("La carga inicial debe bloquear el envío", busyDuringLoad)
        assertEquals(1, requests.size)
        assertEquals("Primero", requests.single().messages.last().content)
        assertFalse(service.isLoading.value)
    }

    @Test fun cancellingAWaitingSendReleasesBusyAndAllowsAnotherMessage() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        coEvery { ai.sendMessage(any()) } returns response("Respuesta nueva")
        val service = ChatAIService(storage, ai, backgroundScope)
        val pending = launch { service.sendMessage("Descartado") }
        runCurrent()
        val wasBusy = service.isLoading.value
        pending.cancelAndJoin()
        assertTrue(wasBusy)
        assertFalse(service.isLoading.value)
        history.complete(emptyList())
        service.sendMessage("Nuevo")
        assertEquals(listOf("Nuevo"), service.messages.value.filter { it.isUser }.map { it.content })
        assertNull(service.error.value)
    }

    @Test fun resetAndNewSendWhileLoadingDoNotReuseTheOldMessageOrClearNewBusyState() = runTest {
        val history = CompletableDeferred<List<Message>?>()
        val reply = CompletableDeferred<ChatResponse>()
        val requests = mutableListOf<ChatRequest>()
        coEvery { storage.loadMessages() } coAnswers { history.await() }
        coEvery { ai.sendMessage(capture(requests)) } coAnswers { reply.await() }
        val service = ChatAIService(storage, ai, backgroundScope)
        val old = launch { service.sendMessage("Anterior") }
        runCurrent()
        service.initSession()
        val fresh = launch { service.sendMessage("Nuevo") }
        runCurrent()
        history.complete(emptyList())
        runCurrent()
        val busyBeforeResponse = service.isLoading.value
        reply.complete(response("Respuesta nueva"))
        old.join()
        fresh.join()
        assertTrue(busyBeforeResponse)
        assertEquals(listOf("Nuevo"), requests.map { it.messages.last { message -> message.isUser }.content })
        assertEquals(listOf("Nuevo"), saved.filter { it.isUser }.map { it.content })
        assertFalse(service.isLoading.value)
    }

    @Test fun lateResponseFromPreviousSessionCannotReplaceCurrentProposalOrLoading() = runTest {
        val oldReply = CompletableDeferred<ChatResponse>()
        val newReply = CompletableDeferred<ChatResponse>()
        coEvery { storage.loadMessages() } returns emptyList()
        coEvery { ai.sendMessage(any()) } coAnswers {
            if (firstArg<ChatRequest>().messages.last { it.isUser }.content == "Anterior") oldReply.await()
            else newReply.await()
        }
        val service = ChatAIService(storage, ai, backgroundScope)
        val repository = AssistantRepository(service, backgroundScope)
        val old = launch { repository.sendMessage("Anterior") }
        runCurrent()
        repository.initSession()
        val fresh = launch { repository.sendMessage("Nuevo") }
        runCurrent()
        oldReply.complete(response("Respuesta antigua"))
        old.join()
        runCurrent()
        val busyBeforeNewReply = service.isLoading.value
        val stalePublished = service.messages.value.any { it.content.contains("Respuesta antigua") }
        newReply.complete(response("Respuesta nueva"))
        fresh.join()
        runCurrent()
        assertTrue(busyBeforeNewReply)
        assertFalse(stalePublished)
        assertTrue(saved.none { it.content.contains("Respuesta antigua") })
        assertEquals("Respuesta nueva", repository.lastResponse.value?.commentary)
    }

    @Test fun lateFailureFromPreviousSessionCannotSetErrorOrStopCurrentLoading() = runTest {
        val oldReply = CompletableDeferred<ChatResponse>()
        val newReply = CompletableDeferred<ChatResponse>()
        coEvery { storage.loadMessages() } returns emptyList()
        coEvery { ai.sendMessage(any()) } coAnswers {
            if (firstArg<ChatRequest>().messages.last { it.isUser }.content == "Anterior") oldReply.await()
            else newReply.await()
        }
        val service = ChatAIService(storage, ai, backgroundScope)
        val old = launch { service.sendMessage("Anterior") }
        runCurrent()
        service.initSession()
        val fresh = launch { service.sendMessage("Nuevo") }
        runCurrent()
        oldReply.completeExceptionally(AIServiceError.EmptyResponse)
        old.join()
        val errorAfterOldReply = service.error.value
        val busyBeforeNewReply = service.isLoading.value
        newReply.complete(response("Respuesta nueva"))
        fresh.join()
        assertNull(errorAfterOldReply)
        assertTrue(busyBeforeNewReply)
        assertNull(service.error.value)
        assertFalse(service.isLoading.value)
    }

    @Test fun slowSaveFromPreviousSessionCannotOverwriteTheNewPersistedConversation() = runTest {
        val oldSaveStarted = CompletableDeferred<Unit>()
        val finishOldSave = CompletableDeferred<Unit>()
        coEvery { storage.loadMessages() } returns emptyList()
        coEvery { ai.sendMessage(any()) } returns response("Respuesta")
        coEvery { storage.saveMessages(any()) } coAnswers {
            val snapshot = firstArg<List<Message>>().toList()
            if (snapshot.any { it.isUser && it.content == "Anterior" }) {
                oldSaveStarted.complete(Unit)
                finishOldSave.await()
            }
            saved = snapshot
        }
        val service = ChatAIService(storage, ai, backgroundScope)
        val old = launch { service.sendMessage("Anterior") }
        oldSaveStarted.await()
        service.initSession()
        val fresh = launch { service.sendMessage("Nuevo") }
        runCurrent()
        finishOldSave.complete(Unit)
        old.join()
        fresh.join()
        runCurrent()
        assertEquals(listOf("Nuevo"), saved.filter { it.isUser }.map { it.content })
        assertFalse(service.isLoading.value)
    }

    private fun proposal(title: String) = """{"propuesta":{"tasks":[{"id":7,"title":"$title","subtasks":[]}]}}"""
    private fun response(comment: String) = ChatResponse(listOf(
        Choice(Message.assistantMessage("""{"comentario":"$comment","propuesta":{"tasks":[]}}"""))
    ))
}
