/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.model

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.ibc.procrastinapp.R

sealed class TaskPriority(@param:StringRes val labelResId: Int) {

    object Low : TaskPriority(R.string.task_priority_low)
    object Normal : TaskPriority(R.string.task_priority_normal)
    object High : TaskPriority(R.string.task_priority_high)
    object Urgent : TaskPriority(R.string.task_priority_urgent)


    companion object {
        fun fromLevel(level: Int): TaskPriority = when (level) {
            0 -> Low
            1 -> Normal
            2 -> High
            3 -> Urgent
            else -> Normal // valor por defecto si hay error
        }
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
}