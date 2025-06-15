/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit

/**
 * Parsea un string de fecha en formato "uuuu-MM-dd HH:mm" o "uuuu-MM-dd"
 * y devuelve un objeto LocalDateTime. El formato "uuuu" representa el
 * año con "era", y es preciso para cuando el parser es STRICT, como aquí
 *
 * @param dateText El string de fecha a parsear
 * @return El objeto LocalDateTime correspondiente, o la fecha actual si hay error
 */
fun parseDateTime(dateText: String): LocalDateTime {
    return try {
        // Formato con hora: "yyyy-MM-dd HH:mm" (16 caracteres)
        if (dateText.length > 10) {
            val formatter = DateTimeFormatter
                .ofPattern("uuuu-MM-dd HH:mm")
                .withResolverStyle(ResolverStyle.STRICT)  // Importante para dar error el 31 de febrero
            LocalDateTime.parse(dateText, formatter)
        }
        // Formato solo fecha: "yyyy-MM-dd" (10 caracteres)
        else {
            val formatter = DateTimeFormatter
                .ofPattern("uuuu-MM-dd")
                .withResolverStyle(ResolverStyle.STRICT)
            val parsed = LocalDate.parse(dateText, formatter)
            // Convertimos a LocalDateTime para usar la función existente
            parsed.atTime(0, 0)
        }
    } catch (_: Exception) {
        // Si falla el parseo por cualquier motivo, devolver la fecha/hora actual
        LocalDateTime.now()
    }
}


fun getDaysFromNow(dateTime: String): Long {
    val today = LocalDate.now()
    val targetDate = parseDateTime(dateTime).toLocalDate()
    return ChronoUnit.DAYS.between(today, targetDate)
}

fun getDaysFromNow(dateTime: LocalDateTime): Long {
    val today = LocalDate.now()
    return ChronoUnit.DAYS.between(today, dateTime)
}

fun getRelativeDateTimeText(dateTime: LocalDateTime): String {
    val today = LocalDate.now()
    val targetDate = dateTime.toLocalDate()
    val daysDiff = getDaysFromNow(dateTime)

    // Verificar si tiene una hora especificada (distinta de 00:00)
    val hasTime = dateTime.hour != 0 || dateTime.minute != 0
    val timeString = if (hasTime) " a las ${formatTime(dateTime)}" else ""

    return when {
        // Casos especiales para días cercanos
        daysDiff == 0L -> "Hoy${timeString}"
        daysDiff == 1L -> "Mañana${timeString}"
        daysDiff == 2L -> "En dos días${timeString}"
        daysDiff == -1L -> "Ayer${timeString}"

        // Mismo mes y año
        targetDate.month == today.month && targetDate.year == today.year ->
            "El día ${targetDate.dayOfMonth}${timeString}"

        // Mismo año, distinto mes
        targetDate.year == today.year ->
            "El ${targetDate.dayOfMonth} de ${formatMonth(targetDate.month)}${timeString}"

        // Distinto año
        else ->
            "El ${targetDate.dayOfMonth} de ${formatMonth(targetDate.month)} de ${targetDate.year}${timeString}"
    }
}

/**
 * Formatea la hora en formato de 24 horas (HH:mm)
 */
private fun formatTime(dateTime: LocalDateTime): String {
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}
private fun formatMonth(month: Month): String {
    return when (month) {
        Month.JANUARY -> "enero"
        Month.FEBRUARY -> "febrero"
        Month.MARCH -> "marzo"
        Month.APRIL -> "abril"
        Month.MAY -> "mayo"
        Month.JUNE -> "junio"
        Month.JULY -> "julio"
        Month.AUGUST -> "agosto"
        Month.SEPTEMBER -> "septiembre"
        Month.OCTOBER -> "octubre"
        Month.NOVEMBER -> "noviembre"
        Month.DECEMBER -> "diciembre"
    }
}
