package com.lifesignal.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "life_signal_prefs"
    private const val THEME_KEY = "app_theme_mode"

    private lateinit var prefs: SharedPreferences

    // 0: System, 1: Light, 2: Dark
    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getInt(THEME_KEY, 0)
    }

    fun setTheme(mode: Int) {
        _themeMode.value = mode
        prefs.edit().putInt(THEME_KEY, mode).apply()
    }
}
