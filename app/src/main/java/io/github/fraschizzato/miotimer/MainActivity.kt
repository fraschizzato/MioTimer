package io.github.fraschizzato.miotimer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    private val gson = Gson()
    private var state by mutableStateOf(TimerState())
    private var screenGraceRunnable: Runnable? = null

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("state")?.let {
                state = gson.fromJson(it, TimerState::class.java)
                updateScreenFlag()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(TimerService.BROADCAST_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                App()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(stateReceiver)
        super.onDestroy()
    }

    private fun updateScreenFlag() {
        val prefs = Prefs(this)

        screenGraceRunnable?.let { window.decorView.removeCallbacks(it) }
        screenGraceRunnable = null

        when {
            prefs.keepScreenOn && state.running -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            !state.running && prefs.keepScreenOn && prefs.endGrace15Seconds -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                screenGraceRunnable = Runnable {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    screenGraceRunnable = null
                }.also { window.decorView.postDelayed(it, 15_000) }
            }

            else -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun send(
        action: String,
        seconds: Int? = null,
        workout: Workout? = null,
    ) {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        seconds?.let { intent.putExtra(TimerService.EXTRA_SECONDS, it) }
        workout?.let { intent.putExtra(TimerService.EXTRA_WORKOUT, gson.toJson(it)) }
        ContextCompat.startForegroundService(this, intent)
    }

    @Composable
    private fun App() {
        var tab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Random", "Countdown", "Workout", "Settings")

        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { },
                            label = { Text(title) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> RandomTimerUi()
                    1 -> CountdownUi()
                    2 -> WorkoutUi()
                    else -> SettingsUi()
                }
            }
        }
    }

    @Composable
    private fun CountdownCard() {
        if (!state.running && !state.completed) return

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(state.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.waitingReps) "RIPETIZIONI" else format(state.remainingMs),
                    style = MaterialTheme.typography.displayMedium,
                )

                if (state.running) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            state.waitingReps -> Button(
                                onClick = { send(TimerService.ACTION_COMPLETE) },
                            ) { Text("Fatto") }

                            state.paused -> Button(
                                onClick = { send(TimerService.ACTION_RESUME) },
                            ) { Text("Riprendi") }

                            else -> Button(
                                onClick = { send(TimerService.ACTION_PAUSE) },
                            ) { Text("Pausa") }
                        }

                        OutlinedButton(onClick = { send(TimerService.ACTION_STOP) }) {
                            Text("Stop")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RandomTimerUi() {
        val prefs = remember { Prefs(this) }
        var min by remember { mutableStateOf(prefs.randomMin.toString()) }
        var max by remember { mutableStateOf(prefs.randomMax.toString()) }

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Random Timer", style = MaterialTheme.typography.headlineLarge)
            Text("Avvia un countdown casuale compreso tra due limiti configurabili.")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = min,
                    onValueChange = { min = it },
                    label = { Text("Min minuti") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = max,
                    onValueChange = { max = it },
                    label = { Text("Max minuti") },
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = {
                    prefs.randomMin = min.toIntOrNull()?.coerceAtLeast(1) ?: 3
                    prefs.randomMax = max.toIntOrNull()?.coerceAtLeast(prefs.randomMin) ?: 6
                    send(TimerService.ACTION_START_RANDOM)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Avvia random")
            }

            CountdownCard()
            Text("Puoi avviare questa modalità anche dal widget MioTimer nella Home Android.")
        }
    }

    @Composable
    private fun CountdownUi() {
        val prefs = remember { Prefs(this) }
        var seconds by remember { mutableStateOf(prefs.countdownSeconds.toString()) }

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Countdown", style = MaterialTheme.typography.headlineLarge)
            OutlinedTextField(
                value = seconds,
                onValueChange = { seconds = it },
                label = { Text("Secondi") },
            )
            Button(
                onClick = {
                    val value = seconds.toIntOrNull()?.coerceAtLeast(1) ?: 60
                    prefs.countdownSeconds = value
                    send(TimerService.ACTION_START_COUNTDOWN, seconds = value)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Avvia countdown")
            }
            CountdownCard()
        }
    }

    @Composable
    private fun WorkoutUi() {
        val prefs = remember { Prefs(this) }
        var workouts by remember {
            mutableStateOf(
                prefs.workouts().ifEmpty {
                    mutableListOf(
                        Workout(
                            "Circuito",
                            mutableListOf(
                                Phase("Esercizio", PhaseType.TIMED, 45, 10),
                                Phase("Recupero", PhaseType.REST, 30, 0),
                            ),
                        ),
                    )
                },
            )
        }
        var selected by remember { mutableIntStateOf(0) }
        val selectedIndex = selected.coerceIn(0, workouts.lastIndex)
        val workout = workouts[selectedIndex]

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Workout", style = MaterialTheme.typography.headlineLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                workouts.forEachIndexed { index, item ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selected = index },
                        label = { Text(item.title.take(16)) },
                    )
                }
                OutlinedButton(
                    onClick = {
                        workouts = (workouts + Workout("Nuova sessione", mutableListOf()))
                            .toMutableList()
                        selected = workouts.lastIndex
                    },
                ) {
                    Text("+")
                }
            }

            OutlinedTextField(
                value = workout.title,
                onValueChange = {
                    workout.title = it
                    workouts = workouts.toMutableList()
                },
                label = { Text("Nome sessione") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        workout.phases.add(Phase("Tempo", PhaseType.TIMED, 45, 0))
                        workouts = workouts.toMutableList()
                    },
                ) { Text("+ Tempo") }

                Button(
                    onClick = {
                        workout.phases.add(Phase("Ripetizioni", PhaseType.REPS, 0, 10))
                        workouts = workouts.toMutableList()
                    },
                ) { Text("+ Reps") }

                Button(
                    onClick = {
                        workout.phases.add(Phase("Recupero", PhaseType.REST, 30, 0))
                        workouts = workouts.toMutableList()
                    },
                ) { Text("+ Rest") }
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(workout.phases) { index, phase ->
                    Card {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OutlinedTextField(
                                value = phase.name,
                                onValueChange = {
                                    phase.name = it
                                    workouts = workouts.toMutableList()
                                },
                                label = { Text("Fase ${index + 1}") },
                            )

                            Text(phase.type.name)

                            if (phase.type == PhaseType.REPS) {
                                OutlinedTextField(
                                    value = phase.reps.toString(),
                                    onValueChange = {
                                        phase.reps = it.toIntOrNull() ?: phase.reps
                                        workouts = workouts.toMutableList()
                                    },
                                    label = { Text("Reps") },
                                )
                            } else {
                                OutlinedTextField(
                                    value = phase.durationSec.toString(),
                                    onValueChange = {
                                        phase.durationSec = it.toIntOrNull() ?: phase.durationSec
                                        workouts = workouts.toMutableList()
                                    },
                                    label = { Text("Secondi") },
                                )
                            }

                            TextButton(
                                onClick = {
                                    workout.phases.removeAt(index)
                                    workouts = workouts.toMutableList()
                                },
                            ) {
                                Text("Rimuovi")
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = workout.phases.isNotEmpty(),
                    onClick = {
                        prefs.saveWorkouts(workouts)
                        send(TimerService.ACTION_START_WORKOUT, workout = workout)
                    },
                ) {
                    Text("Salva + Avvia")
                }
                OutlinedButton(onClick = { prefs.saveWorkouts(workouts) }) {
                    Text("Salva")
                }
            }

            CountdownCard()
        }
    }

    @Composable
    private fun SettingsUi() {
        val prefs = remember { Prefs(this) }
        var keepScreenOn by remember { mutableStateOf(prefs.keepScreenOn) }
        var grace by remember { mutableStateOf(prefs.endGrace15Seconds) }
        var vibrate by remember { mutableStateOf(prefs.vibrate) }
        var sound by remember { mutableStateOf(prefs.sound) }

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Impostazioni", style = MaterialTheme.typography.headlineLarge)
            Toggle("Mantieni schermo acceso durante timer", keepScreenOn) {
                keepScreenOn = it
            }
            Toggle("+15 secondi dopo la fine", grace) { grace = it }
            Toggle("Vibrazione", vibrate) { vibrate = it }
            Toggle("Suono", sound) { sound = it }

            Button(
                onClick = {
                    prefs.keepScreenOn = keepScreenOn
                    prefs.endGrace15Seconds = grace
                    prefs.vibrate = vibrate
                    prefs.sound = sound
                    updateScreenFlag()
                },
            ) {
                Text("Salva")
            }
        }
    }

    @Composable
    private fun Toggle(
        title: String,
        value: Boolean,
        onValueChange: (Boolean) -> Unit,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, Modifier.weight(1f))
            Switch(value, onValueChange)
        }
    }

    private fun format(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}
