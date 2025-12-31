package io.github.kot0kot0.inti.app.data

import android.content.Context
import android.content.SharedPreferences

class IntiSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("inti_prefs", Context.MODE_PRIVATE)

    var whiteLevel: Int
        get() = prefs.getInt("white", 5)
        set(value) = prefs.edit().putInt("white", value).apply()

    var warmLevel: Int
        get() = prefs.getInt("warm", 5)
        set(value) = prefs.edit().putInt("warm", value).apply()

    var durationMinutes: String
        get() = prefs.getString("duration", "60") ?: "60"
        set(value) = prefs.edit().putString("duration", value).apply()
}