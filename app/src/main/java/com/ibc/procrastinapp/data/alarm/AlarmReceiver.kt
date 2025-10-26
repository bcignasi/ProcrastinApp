package com.ibc.procrastinapp.data.alarm // O donde corresponda

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ibc.procrastinapp.R

class AlarmReceiver : BroadcastReceiver() {

    // Define las constantes en un companion object para evitar "magic strings"
    companion object {
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
        private const val ALARM_CHANNEL_ID = "alarm_channel"
        /* TODO: lo de la vibración no funciona, pero creo que es por la configuración del teléfono,
           ya que las notificaciones, fuera de eso, funcionan correctamente
         */

        private val VIBRATION_PATTERN = longArrayOf(0, // Inicio inmediato (silencio inicial = 0)
            1000, 1000, 1000, // Vibración durante 1 segundo, espera 1 segundo, vibración 1 segundo
            500, 500, 500, 500, 500, 500) // Duraciones (ms) silencio-vibración-silencio, etc.
    }

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: context.getString(R.string.default_alarm_message)
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 0) // Recupera el ID

        // Aquí es donde manejas lo que sucede cuando la alarma se dispara.
        // Por ejemplo, mostrar una notificación.
        // Log.d("AlarmReceiver", "Alarm received! Message: $message, ID: $alarmId")
        // Ejemplo: Mostrar una notificación simple
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            context.getString(R.string.alarm_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alarm_notification_channel_description)
            // MEJORA 2: Para una alarma, querrás que vibre y suene.
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Reemplaza con tu ícono
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(message)
            .setVibrate(VIBRATION_PATTERN) // Mismo patrón
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Usar un ID de notificación único si quieres poder actualizarla o si tienes múltiples
        notificationManager.notify(alarmId, notification) // Usa el alarmId para la notificación

        // Aquí podrías también iniciar un servicio, actualizar datos, etc.
    }
}
