package com.example.protivozachatochnye

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

private val Context.dataStore by preferencesDataStore(name = "pill_settings")

private object PillPreferences {
    val pillsInPack = intPreferencesKey("pills_in_pack")
    val startDateEpoch = longPreferencesKey("start_date_epoch")
}

data class PillSettings(
    val pillsInPack: Int?,
    val startDateEpoch: Long?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsFlow: Flow<PillSettings> = dataStore.data.map { preferences ->
                PillSettings(
                    pillsInPack = preferences[PillPreferences.pillsInPack],
                    startDateEpoch = preferences[PillPreferences.startDateEpoch]
                )
            }

            val settings by settingsFlow.collectAsState(initial = PillSettings(null, null))

            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (settings.pillsInPack == null || settings.startDateEpoch == null) {
                    SetupScreen(
                        onSave = { packSize ->
                            saveInitialSettings(packSize)
                        }
                    )
                } else {
                    DayScreen(
                        pillsInPack = settings.pillsInPack,
                        startDateEpoch = settings.startDateEpoch
                    )
                }
            }
        }
    }

    private fun saveInitialSettings(packSize: Int) = runBlocking {
        val today = LocalDate.now().toEpochDay()
        dataStore.edit { preferences ->
            preferences[PillPreferences.pillsInPack] = packSize
            preferences[PillPreferences.startDateEpoch] = today
        }
    }
}

@Composable
private fun SetupScreen(onSave: (Int) -> Unit) {
    var input by remember { mutableStateOf("") }
    val parsed = input.toIntOrNull()
    val isValid = parsed != null && parsed > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добро пожаловать!",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Укажите количество таблеток в упаковке",
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = input,
            onValueChange = { input = it.filter(Char::isDigit) },
            label = { Text("Количество") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(
            onClick = { parsed?.let(onSave) },
            enabled = isValid,
            modifier = Modifier.padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
private fun DayScreen(pillsInPack: Int, startDateEpoch: Long) {
    val todayEpoch = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    val passedDays = (todayEpoch - startDateEpoch).toInt().coerceAtLeast(0)
    val todayPillNumber = (passedDays % pillsInPack) + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сегодня нужно принять таблетку №$todayPillNumber",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "День цикла: ${passedDays + 1}",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Всего таблеток в упаковке: $pillsInPack",
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
