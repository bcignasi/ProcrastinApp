/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service.quotes

import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.ai.ChatRequest
import com.ibc.procrastinapp.data.ai.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList

class QuoteAIService(
    private val aiService: AIService,
    private val coroutineScope: CoroutineScope,
    private val quotePrompt: String
) {
    private val quotesQueue: LinkedList<String> = LinkedList()
    private val mutex = Mutex()

    private val targetSize = 3

    suspend fun initialize() {
        mutex.withLock {
            if (quotesQueue.isEmpty()) {
                repeat(targetSize) {
                    quotesQueue.add(fetchQuoteFromAI())
                }
            }
        }
    }

    suspend fun getNextQuote(): String {
        return mutex.withLock {
            if (quotesQueue.isEmpty()) {
                return@withLock fetchQuoteFromAI()
            }

            val nextQuote = quotesQueue.removeFirst()

            coroutineScope.launch {
                mutex.withLock {
                    if (quotesQueue.size < targetSize) {
                        val newQuote = fetchQuoteFromAI()
                        quotesQueue.addLast(newQuote)
                    }
                }
            }

            return@withLock nextQuote
        }
    }

    private suspend fun fetchQuoteFromAI(): String {
        val chatRequest = ChatRequest(messages = listOf(Message.userMessage(quotePrompt)))
        val response = aiService.sendMessage(chatRequest)
        val assistantMessage = response.choices.firstOrNull()?.message
            ?: throw IllegalStateException("No se recibió respuesta de la IA")
        return assistantMessage.content
    }
}
