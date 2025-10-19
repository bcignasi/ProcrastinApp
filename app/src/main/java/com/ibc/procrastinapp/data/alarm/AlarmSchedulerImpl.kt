package com.ibc.procrastinapp.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.parseNotifyTimeForAlarm


class AlarmSchedulerImpl(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: Task) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // Puedes pasar datos extra al BroadcastReceiver
            putExtra(AlarmReceiver.EXTRA_MESSAGE, task.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, task.id.toInt())
            // Es importante que el action o los data sean únicos por PendingIntent
            // si quieres tener múltiples alarmas distintas.
            // O usar el id en el requestCode del PendingIntent.
            action = "com.ibc.procrastinapp.ALARM_TRIGGERED_${task.id}"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(), // requestCode: debe ser único si quieres actualizar/cancelar esta alarma específica
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //val zonedDateTime = task.notify.atZone(ZoneId.systemDefault())
        //val triggerAtMillis = zonedDateTime.toInstant().toEpochMilli()

        val triggerAtMillis = parseNotifyTimeForAlarm(task.notify)
        if (triggerAtMillis == null) { return }



        // Comprobar si se pueden programar alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // No se pueden programar alarmas exactas.
                // Considera pedir permiso al usuario o usar setWindow() o set() (inexactas)
                // Por ahora, como fallback, usaremos una alarma inexacta.
                // Esto es solo un ejemplo, debes manejar este caso según los requisitos de tu app.
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                // Log.w("AlarmScheduler", "Cannot schedule exact alarms. Scheduling inexact alarm.")
            }
        } else {
            // Para versiones anteriores a Android S (API 31)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        // Log.d("AlarmScheduler", "Alarm scheduled for ID ${alarmItem.id} at ${alarmItem.time}")
    }

    override fun cancel(idTask: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.ibc.procrastinapp.ALARM_TRIGGERED_${idTask}"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            idTask.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        // Log.d("AlarmScheduler", "Alarm canceled for ID ${alarmItem.id}")
    }
}
