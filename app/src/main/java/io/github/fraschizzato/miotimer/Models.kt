package io.github.fraschizzato.miotimer

enum class PhaseType {
    TIMED,
    REPS,
    REST,
}

data class Phase(
    var name: String = "Esercizio",
    var type: PhaseType = PhaseType.TIMED,
    var durationSec: Int = 45,
    var reps: Int = 10,
)

data class Workout(
    var title: String = "Allenamento",
    var phases: MutableList<Phase> = mutableListOf(),
)

data class TimerState(
    val mode: String = "",
    val label: String = "",
    val running: Boolean = false,
    val paused: Boolean = false,
    val remainingMs: Long = 0,
    val phaseIndex: Int = 0,
    val waitingReps: Boolean = false,
    val completed: Boolean = false,
)
