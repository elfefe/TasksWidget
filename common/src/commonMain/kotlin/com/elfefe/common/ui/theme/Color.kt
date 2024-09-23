package com.elfefe.common.ui.theme

import androidx.compose.ui.graphics.Color


val Color.Companion.primary: Color
    get() = Color(0x66222222)
val Color.Companion.onPrimary: Color
    get() = White

val Color.Companion.secondary: Color
    get() = Color(0xFF_005E7D)
val Color.Companion.onSecondary: Color
    get() = White

val Color.Companion.tertiary: Color
    get() = Color(0xFF_B2EBF2)
val Color.Companion.onTertiary: Color
    get() = White

val Color.Companion.background: Color
    get() = White
val Color.Companion.onBackground: Color
    get() = Black

val Color.Companion.surface: Color
    get() = Color(0xFF_E5F5FB)
val Color.Companion.onSurface: Color
    get() = Color(0xFF_002840)

val Color.Companion.error: Color
    get() = Red
val Color.Companion.warning: Color
    get() = Color(0xFF_f5a623)

val Color.Companion.valid: Color
    get() = Color(0xFF_00cc00 )

val String.stringToColor: Color
    get() = Color(
        red = Integer.parseInt(substring(2, 4), 16) / 255f,
        green = Integer.parseInt(substring(4, 6), 16) / 255f,
        blue = Integer.parseInt(substring(6, 8), 16) / 255f,
        alpha = Integer.parseInt(substring(0, 2), 16) / 255f
    )