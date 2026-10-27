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
    private var contextRevision = 0L

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
        contextRevision++
        conversationMessages.clear()

        // Añadimos el mensaje del sistema (prompt)
        conversationMessages.add(Message.systemMessage(taskPrompt))

        // Actualizamos el flujo de mensajes (sin mostrar el del sistema)
        _messagesFlow.value = emptyList()

        // Limpiamos cualquier error
        _error.value = null

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

        initialization.join()

        _error.value = null
        _isLoading.value = true

        val userText = userInput.trim()

        try {
            // Añadir un mensaje del sistema con la hora actual
            val currentDateTime = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )
            Logger.d(logTag, "-> conversationMessages.add(Message.systemMessage(...))")
            conversationMessages.add(Message.systemMessage(
                "La fecha y hora actual es $currentDateTime. " +
                    "El formato vigente es el JSON Schema de response_format: incluye todos sus campos, " +
                    "usa cadenas vacías para textos sin definir y listas vacías cuando no haya tareas " +
                    "o subtareas; no uses null ni texto fuera del JSON."
            ))

            // Creamos y añadimos el mensaje del usuario
            Logger.d(logTag, "-> conversationMessages.add(Message.userMessage(…)")
            conversationMessages.add(Message.userMessage(userText))

            Logger.d(logTag, "-> update _messagesFlow.value")
            _messagesFlow.value = filterUiMessages(conversationMessages)
            Logger.w(logTag, "-> esto (_messagesFlow) debería actualizar la UI")

            // Creamos la solicitud con todos los mensajes (incluido el prompt del sistema)
            val request = ChatRequest(
                model = "gpt-6-luna",
                messages = conversationMessages.toList(),
                responseFormat = AssistantResponseFormat.responseFormat
            )

            // Enviamos la solicitud
            Logger.d(logTag, "-> aiService.sendMessage(request)")
            val response = aiService.sendMessage(request)

            // Extraemos y añadimos la respuesta
            val assistantMessage = response.choices.firstOrNull()?.message
                ?: throw AIServiceError.EmptyResponse

            AssistantResponseFormat.validate(assistantMessage.content)

            Logger.d(logTag, "<- respuesta = $assistantMessage")
            conversationMessages.add(assistantMessage)

            Logger.d(logTag, "update _messagesFlow.value tras añadir assistantMessage")
            Logger.d(logTag, "-> ANTES _messagesFlow.value.size = ${_messagesFlow.value.size}")
            _messagesFlow.value = filterUiMessages(conversationMessages)
            Logger.d(logTag, "-> DESPUES _messagesFlow.value.size = ${_messagesFlow.value.size}")
            Logger.w(logTag, "-> esto (_messagesFlow) debería actualizar la UI")

            // Guardamos los mensajes
            Logger.d(logTag, "saveMessages()")
            saveMessages()

        } catch (e: CancellationException) {
            throw e
        } catch (e: AIServiceError) {

            // Pasamos el error tal cual
            // Los mensajes de error se retrasan hasta llegar al composable
            Logger.e(logTag, "Error de AIService: ${e.message}")
            _error.value = e

        } catch (e: Exception) {
            Logger.e(logTag, "Error inesperado en ChatAIService: ${e.message}")
            _error.value = e    // Este error conserva su tipo Exception

        } finally {
            _isLoading.value = false
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
        if (!initialization.isCompleted) {
            val revision = contextRevision
            coroutineScope.launch {
                initialization.join()
                if (contextRevision == revision) addUserAndAssistantMessage(userMsg, taskJson)
            }
            return
        }

        val logTag = logTag+"IBC11"

        Logger.d(logTag, "-> userMsg = $userMsg")
        Logger.d(logTag, "-> taskJson = $taskJson")

        val userMessage = Message.userMessage(userMsg)
        val assistantMessage = Message.assistantMessage(taskJson)

        // Reabrir el mismo contexto no es una nueva intervención del usuario.
        // Compactamos también las copias consecutivas acumuladas antes de este arreglo.
        // Las respuestas reales y los contextos distintos se conservan.
        while (conversationMessages.size >= 2 &&
            conversationMessages[conversationMessages.lastIndex - 1] == userMessage &&
            conversationMessages.last() == assistantMessage
        ) {
            conversationMessages.removeAt(conversationMessages.lastIndex)
            conversationMessages.removeAt(conversationMessages.lastIndex)
        }

        Logger.d(logTag, "-> conversationMessages.add()")
        conversationMessages.add(userMessage)
        conversationMessages.add(assistantMessage)

        Logger.d(logTag, "-> update _messagesFlow.value")
        _messagesFlow.value = filterUiMessages(conversationMessages)

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

            // Un reinicio durante la lectura tiene prioridad sobre el historial anterior.
            if (contextRevision != 0L) return

            if (messages == null) {
                saveMessages()
                return
            }

            conversationMessages.clear()
            conversationMessages.add(Message.systemMessage(taskPrompt))
            // Las instrucciones y fechas persistidas no deben prevalecer sobre el prompt vigente.
            conversationMessages.addAll(messages.filter { !it.isSystem })

            Logger.w(logTag, "-> update _messagesFlow.value")
            _messagesFlow.value = conversationMessages.filter { it.isUser || it.isAssistant }
            Logger.w(logTag, "-> esto (_messagesFlow) debería actualizar la UI")

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(logTag, "Error general al cargar mensajes: ${e.message}", e)
            _error.value = e
        }
    }

    /**
     * Guarda los mensajes usando el servicio de almacenamiento
     */
    private suspend fun saveMessages() {

        try {
            messageStorage.saveMessages(conversationMessages.toList())
            Logger.d(logTag, "Mensajes guardados correctamente")
        } catch (e: Exception) {
            _error.value = SaveMessagesException(e) // Para distinguirlo del error genérico
            Logger.e(logTag, "${_error.value}")
        }
    }
}
