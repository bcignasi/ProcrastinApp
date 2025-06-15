/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

// Usamos esta función para hacer pruebas de funcionamiento de diferentes partes de la aplicación
fun main() {
    try {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT)
        val date = LocalDate.parse("2023-02-31", formatter)
        println("Fecha parseada: $date") // Este código nunca debería ejecutarse
    } catch (e: Exception) {
        println("Error esperado: ${e.message}")
    }
}