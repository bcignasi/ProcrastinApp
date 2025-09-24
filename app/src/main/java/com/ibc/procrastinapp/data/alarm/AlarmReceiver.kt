package com.ibc.procrastinapp.data.alarm // O donde corresponda

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
//import androidx.privacysandbox.tools.core.generator.build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Alarm!"
        val alarmId = intent.getIntExtra("EXTRA_ALARM_ID", 0) // Recupera el ID

        // Aquí es donde manejas lo que sucede cuando la alarma se dispara.
        // Por ejemplo, mostrar una notificación.
        // Log.d("AlarmReceiver", "Alarm received! Message: $message, ID: $alarmId")

        // Ejemplo: Mostrar una notificación simple
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alarm_channel"

        val channel = NotificationChannel(
            channelId,
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for alarm notifications"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Reemplaza con tu ícono
            .setContentTitle("ProcrastinApp Alarm")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Usar un ID de notificación único si quieres poder actualizarla o si tienes múltiples
        notificationManager.notify(alarmId, notification) // Usa el alarmId para la notificación

        // Aquí podrías también iniciar un servicio, actualizar datos, etc.
    }
}
