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

@JsonAdapter(TaskFieldOrderAdapter::class)
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
        out?.name("primary")?.value(value?.primary?.toArgb()?.let { Integer.toHexString(it) })
        out?.name("onPrimary")?.value(value?.onPrimary?.toArgb()?.let { Integer.toHexString(it) })
        out?.name("secondary")?.value(value?.secondary?.toArgb()?.let { Integer.toHexString(it) })
        out?.name("onSecondary")?.value(value?.onSecondary?.toArgb()?.let { Integer.toHexString(it) })
        out?.name("background")?.value(value?.background?.toArgb()?.let { Integer.toHexString(it) })
        out?.name("onBackground")?.value(value?.onBackground?.toArgb()?.let { Integer.toHexString(it) })
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
            val next = `in`.nextName()
            when (next) {
                "primary" -> primary = `in`.nextString().stringToColor
                "onPrimary" -> onPrimary = `in`.nextString().stringToColor
                "secondary" -> secondary = `in`.nextString().stringToColor
                "onSecondary" -> onSecondary = `in`.nextString().stringToColor
                "background" -> background = `in`.nextString().stringToColor
                "onBackground" -> onBackground = `in`.nextString().stringToColor
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

class TaskFieldOrderAdapter : TypeAdapter<TaskFieldOrder> () {
    override fun write(out: com.google.gson.stream.JsonWriter?, value: TaskFieldOrder?) {
        out?.beginObject()
        out?.name("name")?.value(value?.name)
        out?.name("priority")?.value(value?.priority)
        out?.name("active")?.value(value?.active)
        out?.endObject()
    }

    override fun read(`in`: com.google.gson.stream.JsonReader?): TaskFieldOrder {
        var name = ""
        var priority = 0
        var active = false
        `in`?.beginObject()
        while (`in`?.hasNext() == true) {
            when (`in`.nextName()) {
                "name" -> name = `in`.nextString()
                "priority" -> priority = `in`.nextInt()
                "active" -> active = `in`.nextBoolean()
            }
        }
        `in`?.endObject()
        return TaskFieldOrder(
            name = name,
            priority = priority,
            active = active,
        )
    }
}

