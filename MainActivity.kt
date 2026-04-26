package com.example.pillreminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.pillreminder.alarm.AlarmReceiver
import com.example.pillreminder.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var repo: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = PreferencesRepository(this)
        createNotificationChannel()
        requestPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isSetup by remember { mutableStateOf(true) }
                    var pillNum by remember { mutableIntStateOf(1) }
                    var pillTotal by remember { mutableIntStateOf(21) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        repo.settingsFlow.collect { prefs ->
                            if (prefs != null) {
                                isSetup = prefs[PreferencesRepository.IS_FIRST_LAUNCH] ?: true
                                pillNum = prefs[PreferencesRepository.CURRENT_PILL] ?: 1
                                pillTotal = prefs[PreferencesRepository.PILL_COUNT] ?: 21
                            }
                        }
                    }

                    if (isSetup) {
                        SetupScreen(onSave = { count, current, hour, minute, cycle ->
                            scope.launch {
                                repo.saveSettings(count, current, hour, minute, cycle)
                                scheduleDailyAlarm(hour, minute)
                                isSetup = false
                                pillNum = current
                                pillTotal = count
                            }
                        })
                    } else {
                        MainScreen(pillNum = pillNum, pillTotal = pillTotal, onTake = {
                            scope.launch {
                                repo.confirmPillTaken()
                                withContext(Dispatchers.Main) {
                                    val p = repo.settingsFlow.first()
                                    pillNum = p?.get(PreferencesRepository.CURRENT_PILL) ?: pillNum + 1
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    private fun scheduleDailyAlarm(hour: Int, minute: Int) {
        val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_TRIGGER }
        val pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmMgr.canScheduleExactAlarms()) {
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            }
        } else {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.POST_NOTIFICATIONS] == false) {
            Toast.makeText(this, "Разрешите уведомления для напоминаний", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmMgr.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = android.app.NotificationChannel("pill_channel", "Напоминания", android.app.NotificationManager.IMPORTANCE_HIGH)
        getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
    }
}

@Composable
fun SetupScreen(onSave: (Int, Int, Int, Int, Boolean) -> Unit) {
    var count by remember { mutableStateOf("21") }
    var current by remember { mutableStateOf("1") }
    var hour by remember { mutableStateOf("9") }
    var minute by remember { mutableStateOf("0") }
    var cycle by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Первичная настройка", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("Таблеток в упаковке") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Номер сегодняшней таблетки") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = hour, onValueChange = { hour = it }, label = { Text("Час напоминания (0-23)") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = minute, onValueChange = { minute = it }, label = { Text("Минуты напоминания (0-59)") })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = cycle, onCheckedChange = { cycle = it })
            Text("Повторять по кругу")
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            onSave(count.toInt(), current.toInt(), hour.toInt(), minute.toInt(), cycle)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Сохранить и начать")
        }
    }
}

@Composable
fun MainScreen(pillNum: Int, pillTotal: Int, onTake: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Сегодня необходимо принять:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = "💊 Таблетка №$pillNum", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Осталось в пачке: ${pillTotal - pillNum + 1}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onTake, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("✅ Подтвердить приём", style = MaterialTheme.typography.titleMedium)
        }
    }
}