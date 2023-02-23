package fr.exem.common.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.exem.wavescout.ui.theme.*


private val lightColorScheme = Colors(
    primary = Color.primary,
    primaryVariant = Color.tertiary,
    secondary = Color.secondary,
    secondaryVariant = Color.secondary,
    background = Color.background,
    surface = Color.background,
    onSurface = Color.onBackground,
    onPrimary = Color.onPrimary,
    onSecondary = Color.onSecondary,
    onBackground = Color.onBackground,
    error = Color.error,
    onError = Color.Red,
    isLight = true
)

@Composable
fun SMQToolboxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = lightColorScheme,
        typography = Typography,
        content = content
    )
}

object Theme {
    val color: Colors
        @Composable
        get() = lightColorScheme
}