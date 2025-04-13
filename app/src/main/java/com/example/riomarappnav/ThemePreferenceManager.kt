package com.example.riomarappnav

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class ThemePreferenceManager(private val context: Context) {

    // Vamos usar um SharedPreferences chamado "ThemePrefs"
    private val sharedPrefs = context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)

    // Lê se o usuário forçou modo escuro
    val isDarkModeForced: Boolean
        get() = sharedPrefs.getBoolean("force_dark_mode", false)

    // Ajusta o modo escuro ou claro no SharedPreferences
    // Perceba que NÃO é uma função "suspend"
    fun setDarkMode(forceDark: Boolean) {
        sharedPrefs.edit().putBoolean("force_dark_mode", forceDark).apply()

        // Se você quiser alinhar com o AppCompatDelegate, use:
        if (forceDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            // Se preferir que quando não for dark siga o sistema, troque para MODE_NIGHT_FOLLOW_SYSTEM
            // Se quiser sempre claro, use MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
