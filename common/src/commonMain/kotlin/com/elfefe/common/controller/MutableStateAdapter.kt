package com.elfefe.common.controller

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.*
import kotlinx.metadata.Flag
import java.lang.reflect.Type


class MutableStateAdapter : JsonSerializer<MutableState<*>?>,
    JsonDeserializer<MutableState<*>?> {

    override fun serialize(src: MutableState<*>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement? {
        return context?.serialize(src?.value)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MutableState<*>? {
        val value: Any? = context?.deserialize(json, Any::class.java)
        return mutableStateOf(value) // Assuming you're using mutableStateOf from Jetpack Compose
    }
}
