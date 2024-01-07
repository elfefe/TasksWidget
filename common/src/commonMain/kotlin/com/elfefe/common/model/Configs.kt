package com.elfefe.common.model

import androidx.compose.ui.graphics.Color
import com.elfefe.common.ui.theme.*

data class Configs(
    var taskFieldsOrder: List<TaskFieldOrder> = listOf(
        TaskFieldOrder(name = "title", priority = 0, active = false),
        TaskFieldOrder(name = "description", priority = 0, active = false),
        TaskFieldOrder(name = "deadline", priority = 2, active = true),
        TaskFieldOrder(name = "done", priority = -1, active = true),
        TaskFieldOrder(name = "created", priority = 0, active = true),
        TaskFieldOrder(name = "edited", priority = 0, active = false),
    ),
    var themeColors: ThemeColors = ThemeColors(
        primary = Color.primary,
        onPrimary = Color.onPrimary,
        secondary = Color.secondary,
        onSecondary = Color.onSecondary,
        background = Color.background,
        onBackground = Color.onBackground,
    )
)