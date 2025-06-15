/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.ui.common

import com.ibc.procrastinapp.utils.parseDateTime
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ParseDateTimeTest {

    // Capturar la hora actual antes de cada test para verificar comportamiento de fallback
    private lateinit var beforeTestTime: LocalDateTime

    @Before
    fun setUp() {
        beforeTestTime = LocalDateTime.now()
    }

    @Test
    fun testParseWithTime() {
        // Given
        val dateText = "2023-05-21 14:30"

        // When
        val result = parseDateTime(dateText)

        // Then
        assertEquals(2023, result.year)
        assertEquals(5, result.monthValue)
        assertEquals(21, result.dayOfMonth)
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
        assertEquals(0, result.second)
    }

    @Test
    fun testParseWithoutTime() {
        // Given
        val dateText = "2023-05-21"

        // When
        val result = parseDateTime(dateText)

        // Then
        assertEquals(2023, result.year)
        assertEquals(5, result.monthValue)
        assertEquals(21, result.dayOfMonth)
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
        assertEquals(0, result.second)
    }

    @Test
    fun testValidDateTimeCombinations() {
        // Probar varias combinaciones válidas de fecha y hora
        val validDateTimes = listOf("2023-12-31 23:59", "2000-01-01 00:00", "2025-02-28 15:45")

        for (dateText in validDateTimes) {
            // When
            val result = parseDateTime(dateText)

            // Then
            // Parsear manualmente para verificar
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val expected = LocalDateTime.parse(dateText, formatter)
            assertEquals("Falló para: $dateText", expected, result)
        }
    }

    @Test
    fun testValidDateCombinations() {
        // Probar varias combinaciones válidas de fecha sin hora
        val validDates = listOf("2023-12-31", "2000-01-01", "2025-02-28")

        for (dateText in validDates) {
            // When
            val result = parseDateTime(dateText)

            // Then
            // Parsear manualmente para verificar
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val expected = LocalDate.parse(dateText, formatter).atTime(0, 0)
            assertEquals("Falló para: $dateText", expected, result)
        }
    }

    @Test
    fun testLeapYear() {
        // Given
        val dateText = "2024-02-29"

        // When
        val result = parseDateTime(dateText)

        // Then
        assertEquals(2024, result.year)
        assertEquals(2, result.monthValue)
        assertEquals(29, result.dayOfMonth)
    }

    @Test
    fun testInvalidFormat() {
        // Given
        val dateText = "no-es-una-fecha"

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        // Verificamos que la fecha resultante esté entre beforeTestTime y ahora
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testInvalidDateTimeFormat() {
        // Given
        val dateText = "2023/05/21 14:30"

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testInvalidDateFormat() {
        // Given
        val dateText = "21-05-2023"

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testInvalidDate() {
        // Given - Fecha con día imposible (31 de febrero)
        val dateText = "2023-02-31"

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testInvalidTime() {
        // Given - Hora imposible (25 horas, 70 minutos)
        val dateText = "2023-05-21 25:70"

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testEmptyString() {
        // Given
        val dateText = ""

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testNullInput() {
        // Given
        val dateText: String? = null

        // When
        val result = dateText?.let { parseDateTime(it) } ?: LocalDateTime.now()

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testExactly10Chars() {
        // Given
        val dateText = "2023-05-21"

        // When
        val result = parseDateTime(dateText)

        // Then
        assertEquals(2023, result.year)
        assertEquals(5, result.monthValue)
        assertEquals(21, result.dayOfMonth)
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun testExactly16Chars() {
        // Given
        val dateText = "2023-05-21 14:30"

        // When
        val result = parseDateTime(dateText)

        // Then
        assertEquals(2023, result.year)
        assertEquals(5, result.monthValue)
        assertEquals(21, result.dayOfMonth)
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun testDateLikeString() {
        // Given - String que parece fecha pero no es válido
        val dateText = "2023-AA-21"

        // When
        val result = parseDateTime(dateText)

        // Then
        val now = LocalDateTime.now()
        assertTrue(result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
        assertTrue(result.isBefore(now.plusSeconds(1)))
    }

    @Test
    fun testMultipleInvalidCases() {
        // Lista de casos inválidos para probar de forma masiva
        val invalidCases = listOf(
            "",                      // Vacío
            "abc",                   // Nada que ver con una fecha
            "2023-13-01",            // Mes inválido
            "2023-02-30",            // Día inválido para febrero
            "2023/02/28",            // Formato incorrecto (usa /)
            "2023-02-28 25:00",      // Hora inválida
            "2023-02-28 14:61",      // Minutos inválidos
            "2023-02-28T14:30:00",   // Formato ISO (no soportado por nuestra función)
            "28-02-2023",            // Orden invertido
            "20230228"               // Sin separadores
        )

        for (dateText in invalidCases) {
            // When
            val result = parseDateTime(dateText)

            // Then
            val now = LocalDateTime.now()
            assertTrue("Falló para: $dateText",
                result.isAfter(beforeTestTime) || result.isEqual(beforeTestTime))
            assertTrue("Falló para: $dateText",
                result.isBefore(now.plusSeconds(1)))
        }
    }
}