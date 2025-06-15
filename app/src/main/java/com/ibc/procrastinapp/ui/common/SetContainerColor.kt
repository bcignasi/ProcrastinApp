/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.getDaysFromNow
import java.time.LocalDate

/**
 * Determina el color del contenedor basado en las fechas de deadline y notificación.
 * Si ambas fechas existen, se elige la más próxima para determinar el color.
 *
 * @param task Tarea para la cual se determinará el color
 * @return Color para el contenedor de la Card
 */
@Composable
fun setContainerColor(task: Task): Color {
    val deadline = task.deadline
    if (deadline.isEmpty()) {
        return MaterialTheme.colorScheme.surfaceVariant
    }

    val daysDiff = getDaysFromNow(deadline)
    val isDark = isSystemInDarkTheme()

    val pastAyer     = if (isDark) Color(0xFF897357) else Color(0xFFFFF8F3)
    val pastSemana   = if (isDark) Color(0xFF6F5C3F) else Color(0xFFFFEFDC)
    val pastMes      = if (isDark) Color(0xFF56452A) else Color(0xFFFADEBC)
    val pastLejano   = if (isDark) Color(0xFF3D2E17) else Color(0xFFDDC1A1)
    val todayColor     = if (isDark) Color(0xFF3A506D) else Color(0xFFE3F2FD)
    val tomorrowColor  = if (isDark) Color(0xFF324C7A) else Color(0xFFD0EAFD)
    val plus2Color     = if (isDark) Color(0xFF3E3C8A) else Color(0xFFC7D7F7)
    val weekFuture     = if (isDark) Color(0xFF3B2E70) else Color(0xFFD1C4E9)
    val monthFuture    = if (isDark) Color(0xFF2C1C55) else Color(0xFFB39DDB)
    val farFuture      = if (isDark) Color(0xFF1E0A40) else Color(0xFF9575CD)
    return when {
        daysDiff < -30L -> pastLejano
        daysDiff in -30L..-8L -> pastMes
        daysDiff in -7L..-2L -> pastSemana
        daysDiff == -1L -> pastAyer
        daysDiff == 0L -> todayColor
        daysDiff == 1L -> tomorrowColor
        daysDiff == 2L -> plus2Color
        daysDiff in 3L..7L -> weekFuture
        daysDiff in 8L..30L -> monthFuture
        else -> farFuture
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////

@Preview(name = "Color por tramos - Light", showBackground = true)
@Preview(name = "Color por tramos - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewContainerColorByTramos() {
    val today = LocalDate.now()

    val fakeTasks = listOf(
        Task(title = "Pasado remoto", deadline = today.minusDays(60).toString()),
        Task(title = "Pasado mensual", deadline = today.minusDays(20).toString()),
        Task(title = "Pasado semana", deadline = today.minusDays(5).toString()),
        Task(title = "Ayer", deadline = today.minusDays(1).toString()),
        Task(title = "Hoy", deadline = today.toString()),
        Task(title = "Mañana", deadline = today.plusDays(1).toString()),
        Task(title = "Pasado mañana", deadline = today.plusDays(2).toString()),
        Task(title = "Resto semana", deadline = today.plusDays(5).toString()),
        Task(title = "Resto mes", deadline = today.plusDays(15).toString()),
        Task(title = "Futuro lejano", deadline = today.plusDays(60).toString()),
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(fakeTasks) { task ->
            val color = setContainerColor(task)
            val daysDiff = getDaysFromNow(task.deadline)
            val label = getTimeRangeLabel(daysDiff)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                colors = CardDefaults.cardColors(containerColor = color)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = task.deadline, style = MaterialTheme.typography.bodySmall)
                    Text(text = "Tramo: $label", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun getTimeRangeLabel(daysDiff: Long): String = when {
    daysDiff < -30L -> "Pasado remoto"
    daysDiff in -30L..-8L -> "Pasado mensual"
    daysDiff in -7L..-2L -> "Pasado semana"
    daysDiff == -1L -> "Ayer"
    daysDiff == 0L -> "Hoy"
    daysDiff == 1L -> "Mañana"
    daysDiff == 2L -> "Pasado mañana"
    daysDiff in 3L..7L -> "Resto semana"
    daysDiff in 8L..30L -> "Resto mes"
    else -> "Futuro lejano"
}
