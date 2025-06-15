/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.utils

import android.util.Log
import com.ibc.procrastinapp.BuildConfig

object Logger {

    private val ENABLE_LOGS = BuildConfig.ENABLE_LOGS // Control log outputs

    // Common logging function
    private fun log(
        logLevel: Int,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        truncateLen: Int = 0
    ) {
        if (!ENABLE_LOGS) return

        val stackTrace = Thread.currentThread().stackTrace
        val relevantElement = findRelevantStackTraceElement(stackTrace)

        val formattedMessage = relevantElement?.let {
            val className = it.className.substringAfterLast('.')
            val methodName = it.methodName
            val lineNumber = it.lineNumber
            "[$className.$methodName:$lineNumber] ${
                if (truncateLen > 0) truncateTo(
                    message,
                    truncateLen
                ) else message
            }"
        } ?: message // If no relevant element found, just log the message

        when (logLevel) {
            Log.DEBUG -> Log.d(tag, formattedMessage, throwable)
            Log.WARN -> Log.w(tag, formattedMessage, throwable)
            Log.ERROR -> Log.e(tag, formattedMessage, throwable)
            Log.INFO -> Log.i(tag, formattedMessage)
            Log.VERBOSE -> Log.v(tag, formattedMessage)
        }
    }

    fun d(tag: String, message: String) {
        log(Log.DEBUG, tag, message)
    }

    fun w(tag: String, message: String, truncateLen: Int = 500) {
        log(Log.WARN, tag, message, truncateLen = truncateLen)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.ERROR, tag, message, throwable)
    }

    fun v(tag: String, message: String) {
        log(Log.VERBOSE, tag, message)
    }

    fun i(tag: String, message: String) {
        log(Log.INFO, tag, message)
    }

    private fun findRelevantStackTraceElement(stackTrace: Array<StackTraceElement>): StackTraceElement? {
        val loggerClassName = Logger::class.java.name
        var pastLogger = false

        for (element in stackTrace) {
            if (!pastLogger && element.className == loggerClassName) {
                pastLogger = true // We're now past the Logger's internal calls
            } else if (pastLogger && element.className != loggerClassName) {
                return element // Found the first call outside Logger
            }
        }
        return null // Should ideally not reach here, but handle the case
    }

    private fun truncateTo(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
        }
    }
}

