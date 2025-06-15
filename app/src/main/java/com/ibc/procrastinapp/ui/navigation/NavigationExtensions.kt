/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.navigation

import com.ibc.procrastinapp.utils.Logger

/**
 * Convierte un String de tipo "1,3,5" en una lista de Longs [1, 3, 5].
 * Si algún valor no es convertible, simplemente lo ignora.
 */
fun String.toTaskIdList(): List<Long> {
    Logger.d("IBC11-toTaskIdList", "this: $this")
    val taskIds = this
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }
    Logger.d("IBC11-toTaskIdList", "taskIds: $taskIds")
    return taskIds
}