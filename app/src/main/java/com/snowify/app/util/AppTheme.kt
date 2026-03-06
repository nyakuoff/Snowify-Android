package com.snowify.app.util
enum class AppTheme(val id: String, val displayName: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    OCEAN("ocean", "Ocean"),
    FOREST("forest", "Forest"),
    SUNSET("sunset", "Sunset"),
    ROSE("rose", "Rose"),
    MIDNIGHT("midnight", "Midnight");
    companion object {
        fun fromString(id: String): AppTheme =
            values().firstOrNull { it.id == id } ?: DARK
    }
}
