/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// ChatAIService.kt
package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.ai.ChatRequest
import com.ibc.procrastinapp.data.ai.Message
//import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    // Composición directa - más simple y clara
    private val responseParser = AssistantResponseParserImpl()

    // Prompt especializado para tareas
    private val taskPrompt = buildTaskPrompt()

    // Lista de mensajes de la conversación actual
    private val conversationMessages = mutableListOf<Message>()

    // Flujo de mensajes para la UI (solo mensajes visibles de usuario y asistente)
    private val _messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messagesFlow.asStateFlow()

    // Estado para la última respuesta del asistente
    private val _lastResponse = MutableStateFlow<AssistantResponse?>(null)
    val lastResponse: StateFlow<AssistantResponse?> = _lastResponse.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Inicializamos cargando los mensajes guardados
        Logger.w(logTag, "init: initSession() + loadMessages()")
        initSession()
        coroutineScope.launch {
            loadMessages()
        }
    }

    /**
     * Construye el prompt especializado para tareas
     */
    private fun buildTaskPrompt(): String {
        val currentDateTime = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )
        return """
Contesta en español.

SOY UN ASISTENTE ANTIPROCRASTINACIÓN

Mi función es ayudar al usuario a gestionar tareas, superar la procrastinación y mantener su productividad. La fecha actual es ${currentDateTime}.

OPERACIONES PRINCIPALES:
a) Crear una tarea nueva
b) Modificar una tarea existente
c) Crear una propuesta de tarea desglosada en subtareas
d) Revisar una propuesta de tarea
e) Reagendar tareas o subtareas
f) Consultar fecha y hora actual

REGLA CRÍTICA PARA MANEJO DE IDs:
- Para tareas NUEVAS: usar id=0.
- Para tareas EXISTENTES: SIEMPRE preservar el ID original recibido (cualquier id > 0).
- NUNCA modificar, reemplazar o inventar IDs para tareas existentes.

REGLA CRÍTICA PARA MANEJO DE TAREAS:
- Las tareas se ACUMULAN en una lista de tareas, salvo petición explícita de limpiar.
- Siempre devolverás la lista COMPLETA de tareas.

ESTRUCTURA DE DATOS Y FORMATO JSON REQUERIDO:
{
    "comentario": "Texto con estrategias antiprocrastinación y orientación para el usuario",
    "propuesta": {
        "tasks": [
            {
                "title": "Llamar a Pedro",         // String: resumen corto pero significativo
                "deadline": "2023-12-15 14:30",    // String? (yyyy-MM-dd hh:mm): fecha límite, puede ser null
                "priority": 2,                     // Int: 0-normal, 1-media, 2-alta, 3-urgente
                "periodicity": "1 vez al mes",     // String?: frecuencia de repetición, puede ser null
                "notes": "Cancelar reunión",       // String?: información adicional, puede ser null
                "completed": false,                // Boolean: false-pendiente, true-completada
                "notify": "2023-12-14 10:00",      // String? (yyyy-MM-dd hh:mm): próxima notificación, puede ser null
                "subtasks": []                     // List<Task>?: lista de subtareas, puede ser null o vacía
            },
            {...}                                  // Pueden haber más tareas...
        ]
    }
}

DIRECTRICES ADICIONALES:
- Usar la fecha actual como referencia para ajustar deadlines y notificaciones
- Incluir estrategias antiprocrastinación útiles en el campo "comentario"
- No mezclar el campo "comentario" con la estructura JSON de "propuesta"
- La parte "propuesta" DEBE seguir EXACTAMENTE la estructura JSON especificada

Este manejo correcto de IDs es CRÍTICO para el funcionamiento de la base de datos Room.

En las tareas con subtareas, si no se definen notas, el campo notas se crea 
a partir de los nombres de las subtareas, en líneas separadas.

POR ÚLTIMO
Por favor, recuerda devolver siempre la lista JSON con TODAS las tareas actualizadas.
""".trimIndent()
    }

    /**
     * Inicia una nueva sesión de conversación
     */
    fun initSession() {
        Logger.d(logTag, "initSession() Iniciando nueva sesión")
        conversationMessages.clear()

        // Añadimos el mensaje del sistema (prompt)
        conversationMessages.add(Message.systemMessage(taskPrompt))

        // Actualizamos el flujo de mensajes (sin mostrar el del sistema)
        _messagesFlow.value = emptyList()

        // Limpiamos cualquier error
        _error.value = null
        _lastResponse.value = null

        // Guardamos el estado
        coroutineScope.launch {
            saveMessages()
        }
    }

    /**
     * Envía un mensaje del usuario al asistente y procesa la respuesta
     */
    suspend fun sendMessage(userInput: String) {

        Logger.d(logTag, "-> $userInput")

        if (userInput.isBlank()) return

        _error.value = null
        _isLoading.value = true

        val userInputEnhanced = userInput.trim() +
                " IMPORTANTE: Recuerda darme la lista de tareas actualizadas en formato JSON."

        try {
            // Añadir un mensaje del sistema con la hora actual
            val currentDateTime = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )
            Logger.d(logTag, "-> conversationMessages.add(Message.systemMessage(...))")
            conversationMessages.add(Message.systemMessage("La fecha y hora actual es $currentDateTime"))

            // Creamos y añadimos el mensaje del usuario
            Logger.d(logTag, "-> conversationMessages.add(Message.userMessage(…)")
            conversationMessages.add(Message.userMessage(userInputEnhanced))

            Logger.d(logTag, "-> update _messagesFlow.value")
            _messagesFlow.value = filterUiMessages(conversationMessages)
            Logger.w(logTag, "-> esto (_messagesFlow) debería actualizar la UI")

            // Creamos la solicitud con todos los mensajes (incluido el prompt del sistema)
            val request = ChatRequest(messages = conversationMessages)

            // Enviamos la solicitud
            Logger.d(logTag, "-> aiService.sendMessage(request)")
            val response = aiService.sendMessage(request)

            // Extraemos y añadimos la respuesta
            val assistantMessage = response.choices.firstOrNull()?.message
                ?: throw Exception("No se recibió respuesta de la IA")

            Logger.d(logTag, "<- respuesta = $assistantMessage")
            conversationMessages.add(assistantMessage)

            Logger.d(logTag, "update _messagesFlow.value tras añadir assistantMessage")
            Logger.d(logTag, "-> ANTES _messagesFlow.value.size = ${_messagesFlow.value.size}")
            _messagesFlow.value = filterUiMessages(conversationMessages)
            Logger.d(logTag, "-> DESPUES _messagesFlow.value.size = ${_messagesFlow.value.size}")
            Logger.w(logTag, "-> esto (_messagesFlow) debería actualizar la UI")

            // Procesamos el contenido del mensaje para extraer los componentes
            val messageContent = assistantMessage.content
            val parsedResponse = responseParser.parse(messageContent)
            _lastResponse.value = parsedResponse
            Logger.d(logTag, "_lastResponse.value = $parsedResponse")

            // Guardamos los mensajes
            Logger.d(logTag, "saveMessages()")
            saveMessages()

        } catch (e: Exception) {
            Logger.e(logTag, "Error al enviar mensaje: ${e.message}")
            _error.value = "Error: ${e.message}"

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

        val logTag = logTag+"IBC11"

        Logger.d(logTag, "-> userMsg = $userMsg")
        Logger.d(logTag, "-> taskJson = $taskJson")

        val userMessage = Message.userMessage(userMsg)
        val assistantMessage = Message.assistantMessage(taskJson)

        Logger.d(logTag, "-> conversationMessages.add()")
        conversationMessages.add(userMessage)
        conversationMessages.add(assistantMessage)

        Logger.d(logTag, "-> update _messagesFlow.value")
        _messagesFlow.value = filterUiMessages(conversationMessages)

        // Procesamos el contenido del mensaje para extraer los componentes
        val messageContent = assistantMessage.content
        Logger.d(logTag, "messageContent = $messageContent")
        val parsedResponse = responseParser.parse(messageContent)
        Logger.d(logTag, "parsedResponse = $parsedResponse")

        _lastResponse.value = parsedResponse
        Logger.d(logTag, "-> update _lastResponse.value -> actualiza UI")

        // Guardamos los mensajes
        coroutineScope.launch {
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

            if (messages == null) {
                initSession()
                return
            }

            conversationMessages.clear()
            conversationMessages.addAll(messages)

            // Si no hay mensaje del sistema, lo añadimos
            if (conversationMessages.none { it.isSystem }) {
                conversationMessages.add(0, Message.systemMessage(taskPrompt))
            }

            Logger.w(logTag, "-> update _messagesFlow.value")
            _messagesFlow.value = conversationMessages.filter { it.isUser || it.isAssistant }
            Logger.w(logTag, "-> esto (_messagesFlow) debería actualizar la UI")

        } catch (e: Exception) {
            Logger.e(logTag, "Error general al cargar mensajes: ${e.message}", e)
            // Si hay error, limpiamos e iniciamos una nueva sesión
            initSession()
        }
    }

    /**
     * Guarda los mensajes usando el servicio de almacenamiento
     */
    private suspend fun saveMessages() {

        try {
            messageStorage.saveMessages(conversationMessages)
            Logger.d(logTag, "Mensajes guardados correctamente")
        } catch (e: Exception) {
            _error.value = "Error al guardar la conversación: ${e.message}"
            Logger.e(logTag, "${_error.value}")
        }
    }
}