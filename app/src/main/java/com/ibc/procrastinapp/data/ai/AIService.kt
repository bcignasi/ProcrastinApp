/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// AIService.kt
package com.ibc.procrastinapp.data.ai

import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Servicio centralizado para comunicación con APIs de modelos de lenguaje (OpenAI, Qwen, etc.)
 *
 * Esta clase implementa el patrón Singleton y proporciona una interfaz simplificada
 * para enviar mensajes a modelos de IA y procesar sus respuestas. Se utiliza en la capa
 * de repositorio dentro de la arquitectura MVVM.
 */
class AIService private constructor(apiKey: String, baseUrl: String) {

    private val logTag = "AIService"

    /**
     * Cliente Retrofit para comunicación HTTP con la API
     */
    private val apiClient: AIApiClient

    init {
        // Configuramos el cliente HTTP con autenticación y logging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            // Configuración de timeouts ampliados
            .connectTimeout(30, TimeUnit.SECONDS)    // Tiempo para establecer la conexión
            .readTimeout(120, TimeUnit.SECONDS)      // Tiempo para recibir datos (MUY IMPORTANTE PARA IA)
            .writeTimeout(60, TimeUnit.SECONDS)      // Tiempo para enviar datos
            .callTimeout(180, TimeUnit.SECONDS)      // Tiempo total máximo para la llamada completa
            .build()

        // Construimos el cliente Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiClient = retrofit.create(AIApiClient::class.java)
    }

    /**
     * Interfaz interna que define los endpoints de la API
     */
    // Esta interfaz era privada, pero para poder testear la clase
    // he tenido que dejarla pública
    interface AIApiClient {
        @POST("v1/chat/completions")
        suspend fun sendMessage(@Body chatRequest: ChatRequest): Response<ChatResponse>
    }

    /**
     * Envía un mensaje al modelo de IA y devuelve la respuesta procesada
     *
     * @param request El objeto ChatRequest con los mensajes y configuración
     * @return La respuesta del modelo como ChatResponse
     * @throws Exception Si ocurre un error en la comunicación o procesamiento
     */
    suspend fun sendMessage(request: ChatRequest): ChatResponse = withContext(Dispatchers.IO) {
        try {
            Logger.d(logTag, "Enviando mensaje a la IA: $request")

            val response = apiClient.sendMessage(request)

            if (response.isSuccessful) {
                //return@withContext response.body() ?: throw Exception("Respuesta vacía de la IA")
                return@withContext response.body() ?: throw AIServiceError.EmptyResponse
            } else {
                //throw Exception("Error HTTP ${response.code()}: ${response.errorBody()?.string()}")
                throw AIServiceError.Http(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            Logger.e(logTag, "Error al enviar mensaje a la IA: ${e.message}")
            //throw Exception("Error de comunicación con la IA: ${e.message}", e)
            throw AIServiceError.Communication(e.message, e)
        }
    }

    /**
     * Crea un objeto Singleton para acceso global
     */
    companion object {
        @Volatile
        private var instance: AIService? = null

        // Mensaje interno (no UI): no va a strings.xml
        private const val ERROR_NOT_INITIALIZED =
            "AIService no ha sido inicializado. Llama a AIService.initialize() en el módulo de DI (AppModule.chatAIModule) antes de usar getInstance()."

        /**
         * Inicializa el servicio con los parámetros de configuración
         * Debe llamarse antes de usar getInstance()
         */
        fun initialize(apiKey: String, baseUrl: String = "https://api.openai.com/") {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AIService(apiKey, baseUrl)
                    }
                }
            }
        }

        /**
         * Obtiene la instancia del servicio, previamente inicializada
         */
        fun getInstance(): AIService =
            checkNotNull(instance) { ERROR_NOT_INITIALIZED }
            // checkNotNull sustituye a esto:
            // return instance ?: throw IllegalStateException("AIService no ha sido inicializado. Llama a initialize() primero.")
    }
}

