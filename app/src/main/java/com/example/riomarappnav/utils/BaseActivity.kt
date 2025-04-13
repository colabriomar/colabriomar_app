package com.example.riomarappnav.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.riomarappnav.R
import com.example.riomarappnav.ThemePreferenceManager

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemePreferenceManager(this)
        val isDarkMode = themeManager.isDarkModeForced

        // Evita que o Android use qualquer herança do modo do sistema:
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            setTheme(R.style.Theme_RioMarAppNav_Dark)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            setTheme(R.style.Theme_RioMarAppNav)
        }

        super.onCreate(savedInstanceState)
    }
}

