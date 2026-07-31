package com.example.healthcheckin.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

object ThemeSettings {
    const val THEME_MODE = "theme_mode"
}
