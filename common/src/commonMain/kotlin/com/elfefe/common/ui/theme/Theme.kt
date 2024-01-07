package com.elfefe.common.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


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
fun TasksTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = lightColorScheme,
        typography = Typography,
        content = content
    )
}