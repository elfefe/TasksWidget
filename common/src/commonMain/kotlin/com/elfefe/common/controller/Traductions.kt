package com.elfefe.common.controller

val currentLanguage: String
    get() = Tasks.Configs.configs.language

class Traductions(val language: String = currentLanguage) {
    val emotes: String
        get() = when (language) {
            "fr" -> "Emotes"
            "en" -> "Emotes"
            else -> "Emotes"
        }
    val cards: String
        get() = when (language) {
            "fr" -> "Cartes"
            "en" -> "Cards"
            else -> "Cards"
        }
    val theme: String
        get() = when (language) {
            "fr" -> "Theme"
            "en" -> "Theme"
            else -> "Theme"
        }
    val general: String
        get() = when (language) {
            "fr" -> "Général"
            "en" -> "General"
            else -> "General"
        }
    val startupLabel: String
        get() = when (language) {
            "fr" -> "Lancer l'application au démarrage."
            "en" -> "Launch the application at startup."
            else -> "Launch the application at startup."
        }
}
