package com.example.pillreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pillreminder.R
import com.example.pillreminder.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TRIGGER = "ACTION_TRIGGER"
        const val ACTION_TAKE = "ACTION_TAKE"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TRIGGER -> showNotification(context)
            ACTION_TAKE -> {
                CoroutineScope(Dispatchers.IO).launch { PreferencesRepository(context).confirmPillTaken() }
                cancelNotification(context)
            }
            ACTION_SNOOZE -> {
                snoozeAlarm(context)
                cancelNotification(context)
            }
        }
    }

    private fun showNotification(context: Context) {
        val takeIntent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_TAKE }
        val takePending = PendingIntent.getBroadcast(context, 1, takeIntent, PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SNOOZE }
        val snoozePending = PendingIntent.getBroadcast(context, 2, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "pill_channel")
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("⏰ Пора принять таблетку!")
            .setContentText("Подтвердите приём или отложите на 1 час.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Принять", takePending)
            .addAction(android.R.drawable.ic_menu_recent_history, "Отложить 1ч", snoozePending)
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun snoozeAlarm(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_TRIGGER }
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val time = System.currentTimeMillis() + 3600000L // 1 час

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmMgr.canScheduleExactAlarms()) alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
        } else {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
        }
    }
}