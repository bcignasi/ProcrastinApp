/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// DataStore a nivel de archivo
private val Context.dataStore by preferencesDataStore(name = "tasks_messages")

/**
 * Implementación de MessageStorage que utiliza DataStore para persistir mensajes.
 */
class MessageStorageImpl(
    private val context: Context
) : MessageStorage {
    private val logTag = "MessageStorageImpl"
    private val messagesKey = stringPreferencesKey("task_messages")

    override suspend fun saveMessages(messages: List<Message>) {
        try {
            val json = Json.encodeToString(ListSerializer(Message.serializer()), messages)
            context.dataStore.edit { it[messagesKey] = json }
            Logger.d(logTag, "Mensajes guardados correctamente")
        } catch (e: Exception) {
            Logger.e(logTag, "Error al guardar mensajes: ${e.message}")
            throw e
        }
    }

    override suspend fun loadMessages(): List<Message>? {
        try {
            val json = context.dataStore.data.firstOrNull()?.get(messagesKey) ?: return null

            return try {
                Json.decodeFromString(ListSerializer(Message.serializer()), json)
            } catch (e: Exception) {
                Logger.e(logTag, "Error al analizar mensajes", e)
                null
            }
        } catch (e: Exception) {
            Logger.e(logTag, "Error al cargar mensajes: ${e.message}", e)
            return null
        }
    }
}