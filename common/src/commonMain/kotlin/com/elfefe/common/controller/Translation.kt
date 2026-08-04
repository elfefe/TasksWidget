package com.elfefe.common.controller

val currentLanguage: String
    get() = Tasks.Configs.configs.language

class Translation(val language: String = currentLanguage) {
    val emotes: String
        get() = when (language) {
            "fr" -> "Emotes"
            else -> "Emotes"
        }
    val cards: String
        get() = when (language) {
            "fr" -> "Cartes"
            else -> "Cards"
        }
    val theme: String
        get() = when (language) {
            "fr" -> "Theme"
            else -> "Theme"
        }
    val general: String
        get() = when (language) {
            "fr" -> "Général"
            else -> "General"
        }
    val startupLabel: String
        get() = when (language) {
            "fr" -> "Lancer l'application au démarrage."
            else -> "Launch the application at startup."
        }
    val loginLabel: String
        get() = when (language) {
            "fr" -> "Connexion"
            else -> "Login"
        }
    val registerLabel: String
        get() = when (language) {
            "fr" -> "Inscription"
            else -> "Register"
        }
    val emailLabel: String
        get() = when (language) {
            "fr" -> "Email"
            else -> "Email"
        }
    val passwordLabel: String
        get() = when (language) {
            "fr" -> "Mot de passe"
            else -> "Password"
        }
    val toolbarBackground: String
        get() = when (language) {
            "fr" -> "Arrière-plan de la barre d'outils"
            else -> "Toolbar background"
        }
    val toolbarIcons: String
        get() = when (language) {
            "fr" -> "Icônes de la barre d'outils"
            else -> "Toolbar icons"
        }
    val tasksBackground: String
        get() = when (language) {
            "fr" -> "Arrière-plan des tâches"
            else -> "Tasks background"
        }
    val tasksContent: String
        get() = when (language) {
            "fr" -> "Texte des tâches"
            else -> "Tasks text"
        }
    val color: String
        get() = when (language) {
            "fr" -> "Couleur"
            else -> "Color"
        }
    val grayscale: String
        get() = when (language) {
            "fr" -> "Echelle de gris"
            else -> "Gray scale"
        }
    val brightness: String
        get() = when (language) {
            "fr" -> "Luminositée"
            else -> "Brightness"
        }
    val opacity: String
        get() = when (language) {
            "fr" -> "Opacité"
            else -> "Opacity"
        }
    val accountLabel: String
        get() = when (language) {
            "fr" -> "Compte"
            else -> "Account"
        }
    val accountConnected: String
        get() = when (language) {
            "fr" -> "Connecté"
            else -> "Signed in"
        }
    val accountConnecting: String
        get() = when (language) {
            "fr" -> "Connexion en cours dans le navigateur…"
            else -> "Signing in through your browser…"
        }
    val signInLabel: String
        get() = when (language) {
            "fr" -> "Se connecter avec Google"
            else -> "Sign in with Google"
        }
    val signOutLabel: String
        get() = when (language) {
            "fr" -> "Se déconnecter"
            else -> "Sign out"
        }
    val signInHint: String
        get() = when (language) {
            "fr" -> "Aucun mot de passe à saisir : votre navigateur vous identifie, " +
                    "et l'application se reconnecte seule ensuite."
            else -> "No password to type: your browser identifies you, and the app " +
                    "signs in on its own from then on."
        }
    val resetTheme: String
        get() = when (language) {
            "fr" -> "Rétablir les couleurs d'origine"
            else -> "Restore default colors"
        }
    val holdKeyLabel: String
        get() = when (language) {
            "fr" -> "Touche de maintien"
            else -> "Hold key"
        }
    val holdKeyHint: String
        get() = when (language) {
            "fr" -> "Maintenue, elle empêche la pile de se déployer à l'approche " +
                    "de la souris et permet de déplacer la poignée."
            else -> "Held down, it keeps the stack from expanding when the mouse " +
                    "approaches, and lets you move the handle."
        }
}

