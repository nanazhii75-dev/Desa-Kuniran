package com.desa.kuniran.core.designsystem

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manajer preferensi tema aplikasi Desa Kuniran (Terang / Gelap).
 * Disimpan secara persisten menggunakan SharedPreferences terenkripsi atau privat.
 */
class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kuniran_theme_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean(KEY_DARK_MODE, false)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    companion object {
        private const val KEY_DARK_MODE = "key_theme_dark_mode"
    }
}
