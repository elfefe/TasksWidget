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
    val toolbarBackground: String
        get() = when (language) {
            "fr" -> "Arrière-plan de la barre d'outils"
            "en" -> "Toolbar background"
            else -> "Toolbar background"
        }
    val toolbarIcons: String
        get() = when (language) {
            "fr" -> "Icônes de la barre d'outils"
            "en" -> "Toolbar icons"
            else -> "Toolbar icons"
        }
    val tasksBackground: String
        get() = when (language) {
            "fr" -> "Arrière-plan des tâches"
            "en" -> "Tasks background"
            else -> "Tasks background"
        }
    val tasksContent: String
        get() = when (language) {
            "fr" -> "Texte des tâches"
            "en" -> "Tasks text"
            else -> "Tasks text"
        }
}

