package com.example.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillreminder.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = PreferencesRepository(context).settingsFlow.first()
                if (prefs != null && !(prefs[PreferencesRepository.IS_FIRST_LAUNCH] ?: true)) {
                    val h = prefs[PreferencesRepository.REMINDER_HOUR] ?: 9
                    val m = prefs[PreferencesRepository.REMINDER_MINUTE] ?: 0
                    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val i = Intent(context, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_TRIGGER }
                    val p = android.app.PendingIntent.getBroadcast(context, 0, i, android.app.PendingIntent.FLAG_IMMUTABLE)
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
                        if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (alarmMgr.canScheduleExactAlarms()) alarmMgr.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, p)
                    } else {
                        alarmMgr.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, p)
                    }
                }
            }
        }
    }
}