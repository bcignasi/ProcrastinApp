/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

fun clipAutomaticText(input: String): String {
    val markers = listOf(
        "Aquí tienes la lista actualizada",
        "Aquí te dejo la lista actualizada",
        "```",
        "**Propuesta de Tareas",
        "IMPORTANTE: Recuerda darme"
    )

    val firstMatchIndex = markers
        .mapNotNull { marker -> input.indexOf(marker).takeIf { it >= 0 } }
        .minOrNull()

    return if (firstMatchIndex != null) {
        input.substring(0, firstMatchIndex).trimEnd()
    } else {
        input
    }
}