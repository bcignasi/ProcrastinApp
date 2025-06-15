/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.Logger
import com.ibc.procrastinapp.utils.getDaysFromNow
import com.ibc.procrastinapp.utils.getRelativeDateTimeText
import com.ibc.procrastinapp.utils.parseDateTime

@Composable
fun ShowDatesRow(task: Task) {
    // Si ambos campos están vacíos, no mostramos nada
    if (task.deadline.isEmpty() && task.notify.isEmpty()) {
        return
    }

    // Parseamos las fechas a LocalDateTime
    val deadlineDateTime = task.deadline.takeIf { it.isNotEmpty() }?.let { parseDateTime(it) }
    val notifyDateTime = task.notify.takeIf { it.isNotEmpty() }?.let { parseDateTime(it) }

    // Convertimos a texto para mostrar
    val deadlineText = deadlineDateTime?.let { getRelativeDateTimeText(it) }
    val notifyText = notifyDateTime?.let { getRelativeDateTimeText(it) }



    // Convertimos las fechas a formato relativo si existen
//    val deadlineText = task.deadline.takeIf { it.isNotEmpty() }?.let { getRelativeDateDisplay(it) }
//    val notifyText = task.notify.takeIf { it.isNotEmpty() }?.let { getRelativeDateDisplay(it) }
    val deadlineDiff =
        task.deadline.takeIf { it.isNotEmpty() }?.let { getDaysFromNow(it) }

    Logger.d("IBC17", "ShowDatesRow: deadlineText=$deadlineText, notifyText=$notifyText")
    Logger.d("IBC17", "ShowDatesRow: deadlineDiff=$deadlineDiff")

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Primera fila: Deadline (si existe)
        if (deadlineText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShowDeadline(
                    displayText = deadlineText,
                    //daysDiff = deadlineDiff     // Necesito los días para el color de la tarea
                )
            }
        }

        // Segunda fila: Icono + Notify (si existe)
        if (notifyText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShowIcon()
                Spacer(modifier = Modifier.width(4.dp))
                ShowNotify(displayText = notifyText)
            }
        }
    }
}

@Composable
private fun ShowDeadline(
    displayText: String,
    //daysDiff: Long?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
//        // Círculo para deadline (determinar color según la fecha)
//        //val circleColor = Color.Green //calculateDateColor(daysDiff?:0)
//        val circleColor = calculateDateColor(daysDiff)
//        ColorCircle(
//            color = circleColor,
//            size = 14.dp,
//            modifier = Modifier.padding(end = 6.dp)
//        )

        Text(
            text = displayText,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ShowIcon() {
    Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = "Notificación",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
    )
}

//@Composable
//private fun ColorCircle(
//    color: Color,
//    size: Dp = 12.dp,
//    modifier: Modifier = Modifier
//) {
//    Box(
//        modifier = modifier
//            .size(size)
//            .clip(CircleShape)
//            .background(color)
//    )
//}
//
//// Función auxiliar para determinar el color según la fecha
//private fun calculateDateColor(diffDays: Long?): Color {
//
//    if (diffDays == null)
//        return Color.Transparent
//
//    // Pasado
//    if (diffDays < 0) {
//        return Color(0xFFB20505)
//    }
//
//    // Actuales
//    if (diffDays <= 2) {
//        return Color(0xFFFFAA00) // Amarillo/naranja
//    }
//
//    // Para el resto (fechas futuras)
//    return Color(0xFF179623)
//    //return Color.Green // Verde
//}

//// Función auxiliar para obtener el texto de fecha relativo
//@Composable
//private fun getRelativeDateDisplay(dateText: String): String {
//    return remember(dateText) {
//        try {
//            // Formato con hora: "yyyy-MM-dd HH:mm" (16 caracteres)
//            if (dateText.length > 10) {
//                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
//                val parsed = LocalDateTime.parse(dateText, formatter)
//                getRelativeDateTimeText(parsed)
//            }
//            // Formato solo fecha: "yyyy-MM-dd" (10 caracteres)
//            else {
//                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//                val parsed = LocalDate.parse(dateText, formatter)
//                // Convertimos a LocalDateTime para usar la función existente
//                val dateTime = parsed.atTime(0, 0)
//                getRelativeDateTimeText(dateTime)
//            }
//        } catch (_: Exception) {
//            // Si falla el parseo por cualquier motivo, devolver el texto original
//            dateText
//        }
//    }
//}

@Composable
private fun ShowNotify(displayText: String) {
    Text(
        text = displayText,
        // Opción 1: Estilo más pequeño
        style = MaterialTheme.typography.bodyMedium,

        // Opción 2: Mismo estilo base pero con modificaciones
        // style = MaterialTheme.typography.titleMedium.copy(
        //     fontStyle = FontStyle.Italic,
        //     fontWeight = FontWeight.Normal
        // ),

        // Color diferente para destacar que es una notificación
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    )
}