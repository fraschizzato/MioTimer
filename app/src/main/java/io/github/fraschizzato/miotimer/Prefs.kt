package io.github.fraschizzato.miotimer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Prefs(ctx: Context) {
    private val preferences = ctx.getSharedPreferences("miotimer", Context.MODE_PRIVATE)
    private val gson = Gson()

    var randomMin: Int
        get() = preferences.getInt("random_min", 3)
        set(value) = preferences.edit().putInt("random_min", value).apply()

    var randomMax: Int
        get() = preferences.getInt("random_max", 6)
        set(value) = preferences.edit().putInt("random_max", value).apply()

    var keepScreenOn: Boolean
        get() = preferences.getBoolean("keep_screen_on", true)
        set(value) = preferences.edit().putBoolean("keep_screen_on", value).apply()

    var vibrate: Boolean
        get() = preferences.getBoolean("vibrate", true)
        set(value) = preferences.edit().putBoolean("vibrate", value).apply()

    var sound: Boolean
        get() = preferences.getBoolean("sound", true)
        set(value) = preferences.edit().putBoolean("sound", value).apply()

    var endGrace15Seconds: Boolean
        get() = preferences.getBoolean("end_grace_15_seconds", true)
        set(value) = preferences.edit().putBoolean("end_grace_15_seconds", value).apply()

    var countdownSeconds: Int
        get() = preferences.getInt("countdown_seconds", 60)
        set(value) = preferences.edit().putInt("countdown_seconds", value).apply()

    fun workouts(): MutableList<Workout> {
        val raw = preferences.getString("workouts", null) ?: return mutableListOf()
        return runCatching {
            gson.fromJson<MutableList<Workout>>(
                raw,
                object : TypeToken<MutableList<Workout>>() {}.type,
            )
        }.getOrElse { mutableListOf() }
    }

    fun saveWorkouts(workouts: List<Workout>) {
        preferences.edit().putString("workouts", gson.toJson(workouts)).apply()
    }
}
