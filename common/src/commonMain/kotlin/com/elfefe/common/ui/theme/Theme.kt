package com.elfefe.common.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.elfefe.common.controller.Tasks

/**
 * Theme de l'application, construit a partir des couleurs choisies par
 * l'utilisateur.
 *
 * Il etait fige sur un jeu de constantes : les cartes et les icones lisaient
 * bien la configuration, mais tout ce qui s'en remettait a `MaterialTheme` —
 * champs de texte, boutons, curseurs des reglages — restait sur les couleurs
 * d'origine. Changer de theme n'avait donc qu'un effet partiel.
 */
@Composable
fun TasksTheme(
    content: @Composable () -> Unit
) {
    val themeColors = Tasks.Configs.configs.themeColors
    val colors = remember(themeColors) {
        Colors(
            primary = themeColors.primary,
            primaryVariant = themeColors.primary,
            secondary = themeColors.secondary,
            secondaryVariant = themeColors.secondary,
            background = themeColors.background,
            surface = themeColors.background,
            onSurface = themeColors.onBackground,
            onPrimary = themeColors.onPrimary,
            onSecondary = themeColors.onSecondary,
            onBackground = themeColors.onBackground,
            error = Color.error,
            onError = Color.White,
            // On suit la luminosite du fond choisi pour que Material calcule
            // des surcouches coherentes si l'utilisateur passe au sombre.
            isLight = themeColors.background.perceivedLuminance() > 0.5f
        )
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        content = content
    )
}

/** Luminance percue, pour decider si une couleur de fond est claire. */
private fun Color.perceivedLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
