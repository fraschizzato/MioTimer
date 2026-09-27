package io.github.fraschizzato.miotimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class TimerService : Service() {
    companion object {
        const val ACTION_START_RANDOM = "io.github.fraschizzato.miotimer.action.START_RANDOM"
        const val ACTION_START_COUNTDOWN = "io.github.fraschizzato.miotimer.action.START_COUNTDOWN"
        const val ACTION_START_WORKOUT = "io.github.fraschizzato.miotimer.action.START_WORKOUT"
        const val ACTION_PAUSE = "io.github.fraschizzato.miotimer.action.PAUSE"
        const val ACTION_RESUME = "io.github.fraschizzato.miotimer.action.RESUME"
        const val ACTION_STOP = "io.github.fraschizzato.miotimer.action.STOP"
        const val ACTION_COMPLETE = "io.github.fraschizzato.miotimer.action.COMPLETE"
        const val BROADCAST_STATE = "io.github.fraschizzato.miotimer.STATE"
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_WORKOUT = "workout"

        private const val MODE_RANDOM = "RANDOM"
        private const val MODE_COUNTDOWN = "COUNTDOWN"
        private const val MODE_WORKOUT = "WORKOUT"
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 31
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gson = Gson()

    private var state = TimerState()
    private var endAt = 0L
    private var pausedRemaining = 0L
    private var workout: Workout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "MioTimer",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RANDOM -> startRandomTimer()
            ACTION_START_COUNTDOWN -> startCountdown(
                label = "Countdown",
                milliseconds = intent.getIntExtra(EXTRA_SECONDS, 60) * 1000L,
                mode = MODE_COUNTDOWN,
            )
            ACTION_START_WORKOUT -> {
                val json = intent.getStringExtra(EXTRA_WORKOUT) ?: return START_NOT_STICKY
                startWorkout(json)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_COMPLETE -> completeManualPhase()
            ACTION_STOP -> finish(completed = false)
        }
        return START_STICKY
    }

    private fun foreground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(state.label.ifBlank { "MioTimer" })
            .setContentText(
                if (state.waitingReps) "Completa le ripetizioni" else format(state.remainingMs),
            )
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startRandomTimer() {
        val prefs = Prefs(this)
        val lower = prefs.randomMin.coerceAtLeast(1)
        val upper = prefs.randomMax.coerceAtLeast(lower)
        val minutes = Random.nextInt(lower, upper + 1)
        startCountdown(
            label = "Random · $minutes min",
            milliseconds = minutes * 60_000L,
            mode = MODE_RANDOM,
        )
    }

    private fun startCountdown(label: String, milliseconds: Long, mode: String) {
        scope.coroutineContext.cancelChildren()
        state = TimerState(
            mode = mode,
            label = label,
            running = true,
            paused = false,
            remainingMs = milliseconds,
        )
        endAt = SystemClock.elapsedRealtime() + milliseconds
        foreground()
        scope.launch { tickLoop() }
    }

    private suspend fun tickLoop() {
        while (state.running && !state.paused && !state.waitingReps) {
            val remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            state = state.copy(remainingMs = remaining)
            emitState()

            if (remaining <= 0) {
                if (state.mode == MODE_WORKOUT) {
                    advanceWorkout()
                } else {
                    finish(completed = true)
                }
                break
            }
            delay(250)
        }
    }

    private fun pause() {
        if (!state.running || state.paused || state.waitingReps) return
        pausedRemaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        state = state.copy(paused = true, remainingMs = pausedRemaining)
        scope.coroutineContext.cancelChildren()
        emitState()
    }

    private fun resume() {
        if (!state.running || !state.paused) return
        state = state.copy(paused = false)
        endAt = SystemClock.elapsedRealtime() + pausedRemaining
        scope.launch { tickLoop() }
    }

    private fun startWorkout(json: String) {
        workout = gson.fromJson(json, Workout::class.java)
        state = TimerState(
            mode = MODE_WORKOUT,
            label = workout?.title ?: "Workout",
            running = true,
            phaseIndex = 0,
        )
        foreground()
        runPhase()
    }

    private fun runPhase() {
        val currentWorkout = workout ?: return finish(completed = false)
        if (state.phaseIndex >= currentWorkout.phases.size) {
            finish(completed = true)
            return
        }

        val phase = currentWorkout.phases[state.phaseIndex]
        state = state.copy(
            label = phase.name,
            waitingReps = phase.type == PhaseType.REPS,
            remainingMs = if (phase.type == PhaseType.REPS) 0 else phase.durationSec * 1000L,
        )
        emitState()

        if (phase.type != PhaseType.REPS) {
            endAt = SystemClock.elapsedRealtime() + phase.durationSec * 1000L
            scope.launch { tickLoop() }
        }
    }

    private fun completeManualPhase() {
        if (state.mode == MODE_WORKOUT && state.waitingReps) {
            advanceWorkout()
        }
    }

    private fun advanceWorkout() {
        val currentWorkout = workout ?: return finish(completed = false)
        val nextPhaseIndex = state.phaseIndex + 1

        if (nextPhaseIndex >= currentWorkout.phases.size) {
            finish(completed = true)
            return
        }

        signalCompletion()
        state = state.copy(
            phaseIndex = nextPhaseIndex,
            waitingReps = false,
        )
        runPhase()
    }

    private fun finish(completed: Boolean) {
        scope.coroutineContext.cancelChildren()
        if (completed) signalCompletion()
        state = state.copy(
            running = false,
            completed = completed,
            remainingMs = 0,
        )
        emitState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun signalCompletion() {
        val prefs = Prefs(this)
        if (prefs.vibrate) {
            getSystemService(Vibrator::class.java).vibrate(
                VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
        if (prefs.sound) {
            ToneGenerator(AudioManager.STREAM_ALARM, 75)
                .startTone(ToneGenerator.TONE_PROP_BEEP, 650)
        }
    }

    private fun emitState() {
        sendBroadcast(
            Intent(BROADCAST_STATE)
                .setPackage(packageName)
                .putExtra("state", gson.toJson(state)),
        )
    }

    private fun format(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
