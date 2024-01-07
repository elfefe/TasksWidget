package com.elfefe.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.elfefe.common.controller.getDate
import com.elfefe.common.ui.theme.*
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter

data class Task(
    var title: String = "",
    var description: String = "",
    var deadline: String = getDate(),
    var done: Boolean = false,
    val created: Long = System.currentTimeMillis(),
    var edited: Long = System.currentTimeMillis()
)

data class TaskFieldOrder(
    val name: String,
    var priority: Int,
    var active: Boolean
)

@JsonAdapter(ThemeColorsAdapter::class)
data class ThemeColors(
    var primary: Color,
    var onPrimary: Color,
    var secondary: Color,
    var onSecondary: Color,
    var background: Color,
    var onBackground: Color,
)

class ThemeColorsAdapter : TypeAdapter<ThemeColors> () {
    override fun write(out: com.google.gson.stream.JsonWriter?, value: ThemeColors?) {
        out?.beginObject()
        out?.name("primary")?.value(value?.primary?.toArgb())
        out?.name("onPrimary")?.value(value?.onPrimary?.toArgb())
        out?.name("secondary")?.value(value?.secondary?.toArgb())
        out?.name("onSecondary")?.value(value?.onSecondary?.toArgb())
        out?.name("background")?.value(value?.background?.toArgb())
        out?.name("onBackground")?.value(value?.onBackground?.toArgb())
        out?.endObject()
    }

    override fun read(`in`: com.google.gson.stream.JsonReader?): ThemeColors {
        var primary = Color.primary
        var onPrimary = Color.onPrimary
        var secondary = Color.secondary
        var onSecondary = Color.onSecondary
        var background = Color.background
        var onBackground = Color.onBackground
        `in`?.beginObject()
        while (`in`?.hasNext() == true) {
            when (`in`.nextName()) {
                "primary" -> primary = Color(`in`.nextInt())
                "onPrimary" -> onPrimary = Color(`in`.nextInt())
                "secondary" -> secondary = Color(`in`.nextInt())
                "onSecondary" -> onSecondary = Color(`in`.nextInt())
                "background" -> background = Color(`in`.nextInt())
                "onBackground" -> onBackground = Color(`in`.nextInt())
            }
        }
        `in`?.endObject()
        return ThemeColors(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            background = background,
            onBackground = onBackground,
        )
    }
}

