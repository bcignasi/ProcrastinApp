/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.model

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

sealed class TaskPriority(val level: Int, val label: String) {

    object Low : TaskPriority(0, "Baja")
    object Normal : TaskPriority(1, "Normal")
    object High : TaskPriority(2, "Alta")
    object Urgent : TaskPriority(3, "Urgente")

    companion object {
        fun fromLevel(level: Int): TaskPriority = when (level) {
            0 -> Low
            1 -> Normal
            2 -> High
            3 -> Urgent
            else -> Normal // valor por defecto si hay error
        }

        fun fromLabel(label: String): TaskPriority = when (label.lowercase()) {
            "baja" -> Low
            "media", "normal" -> Normal
            "alta" -> High
            "urgente" -> Urgent
            else -> Normal
        }

        val values = listOf(Low, Normal, High, Urgent)
    }

    fun color(colorScheme: ColorScheme): Color = when (this) {
        Urgent -> colorScheme.error
        High -> colorScheme.error.copy(alpha = 0.6f)
        Normal -> colorScheme.primary
        Low -> colorScheme.onSurfaceVariant
    }

    fun containerColor(colorScheme: ColorScheme): Color = when (this) {
        Urgent -> colorScheme.errorContainer.copy(alpha = 0.8f)
        High -> colorScheme.errorContainer.copy(alpha = 0.4f)
        Normal -> colorScheme.primaryContainer.copy(alpha = 0.7f)
        Low -> colorScheme.surfaceVariant
    }

    override fun toString(): String = label
}