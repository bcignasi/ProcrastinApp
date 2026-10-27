/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// ChatAIService.kt
package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.ai.AIServiceError
import com.ibc.procrastinapp.data.ai.AssistantResponseFormat
import com.ibc.procrastinapp.data.ai.ChatRequest
import com.ibc.procrastinapp.data.ai.GoalPlanningPrompt
import com.ibc.procrastinapp.data.ai.Message
//import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// Para distinguir el error de salvado de mensajes del Exception genérico
class SaveMessagesException(cause: Throwable) : Exception(cause)

/**
 * Implementación simplificada de servicio para la gestión de tareas mediante IA.
 * Combina funcionalidades de persistencia, comunicación con IA y procesamiento de mensajes.
 *
 * Sigue un enfoque MVVM reduciendo las capas de abstracción.
 */
class ChatAIService(
    ////private val context: Context,
    private val messageStorage: MessageStorage,
    private val aiService: AIService,
    private val coroutineScope: CoroutineScope
) {
    private val logTag = "IBC-ChatAIService"

    // Prompt especializado para tareas
    private val taskPrompt = GoalPlanningPrompt.text

    // Lista de mensajes de la conversación actual
    private val conversationMessages = mutableListOf(Message.systemMessage(taskPrompt))
    // El arranque se ejecuta en IO y las acciones pueden llegar desde Main.
    private val stateLock = Any()
    private val persistenceMutex = Mutex()
    private var contextRevision = 0L
    private var editRevision = 0L
    private var activeRequest: Any? = null

    // Flujo de mensajes para la UI (solo mensajes visibles de usuario y asistente)
    private val _messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messagesFlow.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error (cambio de String a Throwable (no me vale AIService error
    // porque quiero incluir una nueva excepción genérica en esta clase)
    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    // Restaurar antes de permitir escrituras evita sobrescribir el historial al arrancar.
    private val initialization = coroutineScope.launch { loadMessages() }

    /**
     * Inicia una nueva sesión de conversación
     */
    fun initSession() {
        Logger.d(logTag, "initSession() Iniciando nueva sesión")
        synchronized(stateLock) {
            contextRevision++
            editRevision++
            activeRequest = null
            _isLoading.value = false
            conversationMessages.clear()
            conversationMessages.add(Message.systemMessage(taskPrompt))
            _messagesFlow.value = emptyList()
            _error.value = null
        }

        // Guardamos el estado
        coroutineScope.launch {
            initialization.join()
            saveMessages()
        }
    }

    /**
     * Envía un mensaje del usuario al asistente y procesa la respuesta
     */
    suspend fun sendMessage(userInput: String) {

        Logger.d(logTag, "-> $userInput")

        if (userInput.isBlank()) return

        val requestToken = Any()
        synchronized(stateLock) {
            if (activeRequest != null) return
            activeRequest = requestToken
            _error.value = null
            _isLoading.value = true
        }
        val userText = userInput.trim()

        try {
            initialization.join()
            // Añadir un mensaje del sistema con la hora actual
            val currentDateTime = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )
            val request = synchronized(stateLock) {
                // Un reinicio invalida también los envíos que aún esperaban al historial.
                if (activeRequest !== requestToken) return
                conversationMessages.add(Message.systemMessage(
                    "La fecha y hora actual es $currentDateTime. " +
                        "El formato vigente es el JSON Schema de response_format: incluye todos sus campos, " +
                        "usa cadenas vacías para textos sin definir y listas vacías cuando no haya tareas " +
                        "o subtareas; no uses null ni texto fuera del JSON."
                ))
                conversationMessages.add(Message.userMessage(userText))
                _messagesFlow.value = filterUiMessages(conversationMessages)
                ChatRequest(
                    model = "gpt-6-luna",
                    messages = conversationMessages.toList(),
                    responseFormat = AssistantResponseFormat.responseFormat
                )
            }

            // Enviamos la solicitud
            Logger.d(logTag, "-> aiService.sendMessage(request)")
            val response = aiService.sendMessage(request)

            // Extraemos y añadimos la respuesta
            val assistantMessage = response.choices.firstOrNull()?.message
                ?: throw AIServiceError.EmptyResponse

            AssistantResponseFormat.validate(assistantMessage.content)

            synchronized(stateLock) {
                if (activeRequest !== requestToken) return
                conversationMessages.add(assistantMessage)
                _messagesFlow.value = filterUiMessages(conversationMessages)
            }

            // Guardamos los mensajes
            Logger.d(logTag, "saveMessages()")
            saveMessages()

        } catch (e: CancellationException) {
            throw e
        } catch (e: AIServiceError) {

            // Pasamos el error tal cual
            // Los mensajes de error se retrasan hasta llegar al composable
            Logger.e(logTag, "Error de AIService: ${e.message}")
            synchronized(stateLock) {
                if (activeRequest === requestToken) _error.value = e
            }

        } catch (e: Exception) {
            Logger.e(logTag, "Error inesperado en ChatAIService: ${e.message}")
            synchronized(stateLock) {
                if (activeRequest === requestToken) _error.value = e
            }

        } finally {
            synchronized(stateLock) {
                if (activeRequest === requestToken) {
                    activeRequest = null
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Añade una pareja de mensajes del usuario y mensaje Assistant a la lista de mensajes
     * El mensaje será el JSON de una Task (pero ChatAIService no tiene por qué saberlo)
     * La idea es que al seleccionar una Task para editar en TaskListScreen, el id de la
     * Task se pasa a AssistantScreen, y este pida la tarea a su ViewModel, el cual la
     * convertirá en JSON y la inyectará mediante esta función en la lista de mensajes.
     * De esta forma, aparecerá en la lista de mensajes del asistente como si la hubiera devuelto
     * ChatGPT y de este modo podremos editarla.
     *
     * ACTUALIZA conversationMessages y lastResponse
     */

    fun addUserAndAssistantMessage(userMsg: String, taskJson: String) {
        val revision = synchronized(stateLock) { ++editRevision }
        if (!initialization.isCompleted) {
            coroutineScope.launch {
                initialization.join()
                appendEditingMessages(userMsg, taskJson, revision)
            }
            return
        }
        appendEditingMessages(userMsg, taskJson, revision)
    }

    fun cancelPendingEdit() {
        synchronized(stateLock) { editRevision++ }
    }

    private fun appendEditingMessages(userMsg: String, taskJson: String, revision: Long) {
        val userMessage = Message.userMessage(userMsg)
        val assistantMessage = Message.assistantMessage(taskJson)
        synchronized(stateLock) {
            if (editRevision != revision) return
            // Reabrir el mismo contexto no es una nueva intervención del usuario.
            while (conversationMessages.size >= 2 &&
                conversationMessages[conversationMessages.lastIndex - 1] == userMessage &&
                conversationMessages.last() == assistantMessage
            ) {
                conversationMessages.removeAt(conversationMessages.lastIndex)
                conversationMessages.removeAt(conversationMessages.lastIndex)
            }
            conversationMessages.add(userMessage)
            conversationMessages.add(assistantMessage)
            _messagesFlow.value = filterUiMessages(conversationMessages)
        }

        // Guardamos los mensajes
        coroutineScope.launch {
            initialization.join()
            saveMessages()
        }
    }

    /**
     * Actualiza la lista de mensajes visibles (excluyendo mensajes del sistema)
     */
    private fun filterUiMessages(conversationMessages: List<Message>) : List<Message> {

        val retValue = conversationMessages.filter { it.isUser || it.isAssistant }
        Logger.d(logTag, "Mensajes actualizados (size): ${retValue.size}")
        Logger.d(logTag, "Mensajes actualizados (tail): ${retValue.toString().takeLast(500)}")
        return retValue
    }

    /**
     * Carga los mensajes guardados (actualiza conversationMessages)
     */
    private suspend fun loadMessages() {

        try {
            val messages = messageStorage.loadMessages()

            synchronized(stateLock) {
                // La comprobación y restauración son atómicas respecto al reinicio.
                if (contextRevision != 0L) return
                if (messages != null) {
                    conversationMessages.clear()
                    conversationMessages.add(Message.systemMessage(taskPrompt))
                    conversationMessages.addAll(messages.filter { !it.isSystem })
                    _messagesFlow.value = filterUiMessages(conversationMessages)
                }
            }
            if (messages == null) saveMessages()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(logTag, "Error general al cargar mensajes: ${e.message}", e)
            synchronized(stateLock) {
                if (contextRevision == 0L) _error.value = e
            }
        }
    }

    /**
     * Guarda los mensajes usando el servicio de almacenamiento
     */
    private suspend fun saveMessages() {
        // Tomar la copia dentro de la cola evita que un guardado viejo termine el último.
        persistenceMutex.withLock {
            val (revision, snapshot) = synchronized(stateLock) {
                contextRevision to conversationMessages.toList()
            }
            try {
                messageStorage.saveMessages(snapshot)
                Logger.d(logTag, "Mensajes guardados correctamente")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                synchronized(stateLock) {
                    if (contextRevision == revision) _error.value = SaveMessagesException(e)
                }
                Logger.e(logTag, "Error al guardar mensajes", e)
            }
        }
    }
}
