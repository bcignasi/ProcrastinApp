/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

// La clase de datos separada de la interfaz para mayor claridad
data class AssistantResponse(
    val text: String = "",  // Renombrado para mayor claridad
    val json: String = "",       // Renombrado para mayor claridad
    val commentary: String = ""  // Renombrado para mayor claridad
) {

    val hasJson: Boolean get() = json.isNotBlank()
    val hasCommentary: Boolean get() = commentary.isNotBlank()
    val hasText: Boolean get() = text.isNotBlank()
}