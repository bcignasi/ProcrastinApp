/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.data.ai.Message

/**
 * Interfaz para la persistencia de mensajes de chat.
 * Abstrae el mecanismo de almacenamiento para facilitar testing y cambios futuros.
 */
interface MessageStorage {
    suspend fun saveMessages(messages: List<Message>)
    suspend fun loadMessages(): List<Message>?
}