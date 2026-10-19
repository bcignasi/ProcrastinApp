package com.ibc.procrastinapp.ui.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.format.ResolverStyle
import java.util.Locale

private val storedDateTime = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    .withResolverStyle(ResolverStyle.STRICT)
private val clockTime = DateTimeFormatter.ofPattern("HH:mm")

/** La hora basta para hoy; las fechas sin hora nunca se convierten en las 00:00. */
internal fun flatTaskDateText(value: String, referenceDate: LocalDate, locale: Locale): String? {
    if (value.isBlank()) return null
    return try {
        val date = LocalDate.parse(value.take(10))
        val time = if (value.length > 10) LocalDateTime.parse(value, storedDateTime).format(clockTime) else null
        if (date == referenceDate && time != null) {
            time
        } else {
            val dateText = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            listOfNotNull(dateText, time).joinToString(" · ")
        }
    } catch (_: DateTimeParseException) {
        value // Conservar el dato recibido, sin inventar una fecha actual.
    }
}
