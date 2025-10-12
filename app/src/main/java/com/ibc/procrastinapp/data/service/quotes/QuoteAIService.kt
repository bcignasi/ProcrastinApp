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

/**
 * Manages fetching and caching of motivational quotes from an AI service.
 *
 * This service maintains an internal queue of quotes to minimize latency and reduce
 * the number of calls to the AI service. It pre-populates the queue and replenishes
 * it in the background as quotes are consumed.
 *
 * @param aiService The service for interacting with the AI model.
 * @param coroutineScope The coroutine scope for launching background tasks.
 * @param quotePrompt The prompt used to request a quote from the AI.
 */
class QuoteAIService(
    private val aiService: AIService,
    private val coroutineScope: CoroutineScope,
    private val quotePrompt: String
) {
    private val quotesQueue: LinkedList<String> = LinkedList()
    private val mutex = Mutex()

    private val targetSize = 3

    /**
     * Initializes the service by pre-populating the quote queue.
     *
     * If the queue is empty, this function fetches an initial set of quotes to ensure
     * they are readily available for the first requests. This should be called
     * during application startup.
     */
    suspend fun initialize() {
        mutex.withLock {
            if (quotesQueue.isEmpty()) {
                repeat(targetSize) {
                    fetchQuoteFromAI()?.let { quotesQueue.add(it) }
                }
            }
        }
    }

    /**
     * Retrieves the next motivational quote.
     *
     * This function takes a quote from the head of the queue. If the queue becomes low,
     * it triggers a background task to fetch more quotes. If the queue is empty,
     * it attempts to fetch a new quote synchronously.
     *
     * @return A [String] containing the motivational quote. Returns an empty string if
     * a quote cannot be fetched.
     */
    suspend fun getNextQuote(): String {
        return mutex.withLock {
            if (quotesQueue.isEmpty()) {
                return@withLock fetchQuoteFromAI() ?: "" // Si no hay frases, no presenta nada
            }

            val nextQuote = quotesQueue.removeFirst()

            coroutineScope.launch {
                mutex.withLock {
                    if (quotesQueue.size < targetSize) {
                        fetchQuoteFromAI()?.let { newQuote ->
                            quotesQueue.addLast(newQuote)
                        }
                    }
                }
            }

            return@withLock nextQuote
        }
    }

    /**
     * Fetches a single quote from the AI service.
     *
     * This function handles the network request and parsing of the response.
     * It gracefully handles errors by returning null.
     *
     * @return The fetched quote as a [String], or `null` if an error occurs.
     */
    private suspend fun fetchQuoteFromAI(): String? {
        return try {
            val chatRequest = ChatRequest(messages = listOf(Message.userMessage(quotePrompt)))
            val response = aiService.sendMessage(chatRequest)
            response.choices.firstOrNull()?.message?.content
        } catch (_: Exception) {
            null
        }
    }
}
