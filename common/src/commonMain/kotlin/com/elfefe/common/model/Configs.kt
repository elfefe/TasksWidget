package com.elfefe.common.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.elfefe.common.ui.theme.*
import com.google.common.reflect.TypeToken
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter


@JsonAdapter(ConfigsAdapter::class)
class Configs(
    _taskFieldsOrder: List<TaskFieldOrder> = listOf(
        TaskFieldOrder(name = "title", priority = 0, active = false),
        TaskFieldOrder(name = "description", priority = 0, active = false),
        TaskFieldOrder(name = "deadline", priority = 2, active = true),
        TaskFieldOrder(name = "done", priority = -1, active = true),
        TaskFieldOrder(name = "created", priority = 0, active = true),
        TaskFieldOrder(name = "edited", priority = 0, active = false),
    ),
    _themeColors: ThemeColors = ThemeColors(
        primary = Color.primary,
        onPrimary = Color.onPrimary,
        secondary = Color.secondary,
        onSecondary = Color.onSecondary,
        background = Color.background,
        onBackground = Color.onBackground,
    )
) {
    var themeColors: ThemeColors by mutableStateOf(_themeColors)
    var taskFieldsOrder: List<TaskFieldOrder> by mutableStateOf(_taskFieldsOrder)

    fun updateThemeColors(
        primary: Color = themeColors.primary,
        onPrimary: Color = themeColors.onPrimary,
        secondary: Color = themeColors.secondary,
        onSecondary: Color = themeColors.onSecondary,
        background: Color = themeColors.background,
        onBackground: Color = themeColors.onBackground
    ) {
        themeColors = ThemeColors(primary, onPrimary, secondary, onSecondary, background, onBackground)
    }

    fun updateTaskFieldsOrder(taskFieldsOrder: List<TaskFieldOrder>) {
        this.taskFieldsOrder = taskFieldsOrder
    }

    override fun toString(): String {
        return "Configs(themeColors=$themeColors, taskFieldsOrder=$taskFieldsOrder)"
    }
}


class ConfigsAdapter : TypeAdapter<Configs>() {
    override fun write(out: JsonWriter?, value: Configs?) {
        out?.setIndent("  ")
        out?.isLenient = true
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        gson.toJson(JsonObject().apply {
            add("orders", gson.toJsonTree(value?.taskFieldsOrder))
            add("colors", gson.toJsonTree(value?.themeColors))
        }, out)
    }

    override fun read(`in`: com.google.gson.stream.JsonReader?): Configs {
        `in`?.beginObject()
        if (`in`?.nextName() == "orders") throw JsonParseException("Expected 'orders' as first field")
        `in`?.beginArray()
        val taskFieldsOrders = mutableListOf<TaskFieldOrder>()
        while (`in`?.peek() != JsonToken.END_ARRAY)
            taskFieldsOrders.add(TaskFieldOrderAdapter().read(`in`))
        `in`.endArray()
        if (`in`.nextName() == "colors") throw JsonParseException("Expected 'colors' as second field")
        val themeColors = ThemeColorsAdapter().read(`in`)
        `in`.endObject()
        return Configs(
            _taskFieldsOrder = taskFieldsOrders,
            _themeColors = themeColors,
        )
    }
}

