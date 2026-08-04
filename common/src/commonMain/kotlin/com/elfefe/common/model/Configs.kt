package com.elfefe.common.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import com.elfefe.common.ui.theme.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/** Touche maintenue qui retient la pile au lieu de la déployer. */
enum class HoldKey(val id: String, val label: String) {
    CONTROL("ctrl", "Ctrl"),
    ALT("alt", "Alt"),
    SHIFT("shift", "Maj");

    companion object {
        fun of(id: String?): HoldKey = values().firstOrNull { it.id == id } ?: CONTROL
    }
}

/** Position verticale libre de la poignée ; -1 signifie « centrée d'office ». */
const val HANDLE_Y_AUTO = -1f

@JsonAdapter(ConfigsAdapter::class)
class Configs(
    taskFieldsOrder: List<TaskFieldOrder> = listOf(
        TaskFieldOrder(name = "title", priority = 0, active = false),
        TaskFieldOrder(name = "description", priority = 0, active = false),
        TaskFieldOrder(name = "deadline", priority = 2, active = true),
        TaskFieldOrder(name = "done", priority = -1, active = true),
        TaskFieldOrder(name = "created", priority = 0, active = true),
        TaskFieldOrder(name = "edited", priority = 0, active = false),
    ),
    themeColors: ThemeColors = defaultThemeColors(),
    language: String = Locale.current.language,
    holdKey: HoldKey = HoldKey.CONTROL,
    handleY: Float = HANDLE_Y_AUTO,
    anchorRight: Boolean = true,
    pinned: Boolean = false,
    font: AppFont = AppFont.INTER
) {
    var taskFieldsOrder: List<TaskFieldOrder> by mutableStateOf(taskFieldsOrder)
        private set
    var themeColors: ThemeColors by mutableStateOf(themeColors)
        private set
    var language: String by mutableStateOf(language)
        private set

    /** Touche qui, maintenue, empêche le déploiement et libère la poignée. */
    var holdKey: HoldKey by mutableStateOf(holdKey)
        private set

    /** Ordonnée choisie pour la poignée, ou [HANDLE_Y_AUTO]. */
    var handleY: Float by mutableStateOf(handleY)
        private set

    /** Bord d'ancrage de la pile : droite par défaut. */
    var anchorRight: Boolean by mutableStateOf(anchorRight)
        private set

    /** Pile épinglée : elle reste déployée quoi qu'il arrive. */
    var pinned: Boolean by mutableStateOf(pinned)
        private set

    /** Police de l'interface. */
    var font: AppFont by mutableStateOf(font)
        private set

    /**
     * Prévenu à chaque changement pour que la configuration soit écrite sur
     * disque. Sans ce rappel, modifier une couleur n'enregistrait rien : les
     * mutateurs changent l'intérieur de l'objet, jamais la référence, et seul le
     * remplacement de la référence déclenchait la sauvegarde.
     */
    var onChanged: (() -> Unit)? = null

    fun updateThemeColors(
        primary: Color = themeColors.primary,
        onPrimary: Color = themeColors.onPrimary,
        secondary: Color = themeColors.secondary,
        onSecondary: Color = themeColors.onSecondary,
        background: Color = themeColors.background,
        onBackground: Color = themeColors.onBackground
    ) {
        themeColors = ThemeColors(primary, onPrimary, secondary, onSecondary, background, onBackground)
        onChanged?.invoke()
    }

    /** Rétablit les couleurs livrées avec l'application. */
    fun resetThemeColors() {
        themeColors = defaultThemeColors()
        onChanged?.invoke()
    }

    fun updateTaskFieldsOrder(taskFieldsOrder: List<TaskFieldOrder>) {
        this.taskFieldsOrder = taskFieldsOrder
        onChanged?.invoke()
    }

    fun updateLanguage(language: String) {
        this.language = language
        onChanged?.invoke()
    }

    fun updateHoldKey(holdKey: HoldKey) {
        this.holdKey = holdKey
        onChanged?.invoke()
    }

    /** Position de la poignée après un déplacement à la souris. */
    fun updateHandlePlacement(handleY: Float, anchorRight: Boolean) {
        this.handleY = handleY
        this.anchorRight = anchorRight
        onChanged?.invoke()
    }

    /** Épingle ou libère la pile ; l'état survit au redémarrage. */
    fun updatePinned(pinned: Boolean) {
        this.pinned = pinned
        onChanged?.invoke()
    }

    fun updateFont(font: AppFont) {
        this.font = font
        onChanged?.invoke()
    }

    override fun toString(): String =
        "Configs(themeColors=$themeColors, taskFieldsOrder=$taskFieldsOrder, language=$language, " +
                "holdKey=${holdKey.id}, handleY=$handleY, anchorRight=$anchorRight, pinned=$pinned, " +
                "font=${font.id})"

    companion object {
        fun defaultThemeColors(): ThemeColors = ThemeColors(
            primary = Color.primary,
            onPrimary = Color.onPrimary,
            secondary = Color.secondary,
            onSecondary = Color.onSecondary,
            background = Color.background,
            onBackground = Color.onBackground,
        )
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
            add("language", gson.toJsonTree(value?.language))
            add("holdKey", gson.toJsonTree(value?.holdKey?.id))
            add("handleY", gson.toJsonTree(value?.handleY))
            add("anchorRight", gson.toJsonTree(value?.anchorRight))
            add("pinned", gson.toJsonTree(value?.pinned))
            add("font", gson.toJsonTree(value?.font?.id))
        }, out)
    }

    override fun read(`in`: com.google.gson.stream.JsonReader?): Configs {
        val taskFieldsOrders = mutableListOf<TaskFieldOrder>()
        var themeColors: ThemeColors = Configs.defaultThemeColors()
        var language: String = Locale.current.language
        var holdKey = HoldKey.CONTROL
        var handleY = HANDLE_Y_AUTO
        var anchorRight = true
        var pinned = false
        var font = AppFont.INTER
        `in`?.beginObject()
        while (`in`?.hasNext() == true) {
            when (`in`.nextName()) {
                "orders" -> {
                    `in`.beginArray()
                    while (`in`.peek() != JsonToken.END_ARRAY)
                        taskFieldsOrders.add(TaskFieldOrderAdapter().read(`in`))
                    `in`.endArray()
                }
                "colors" -> themeColors = ThemeColorsAdapter().read(`in`)
                "language" -> language = `in`.nextString()
                "holdKey" -> holdKey = HoldKey.of(`in`.nextString())
                "handleY" -> handleY = `in`.nextDouble().toFloat()
                "anchorRight" -> anchorRight = `in`.nextBoolean()
                "pinned" -> pinned = `in`.nextBoolean()
                "font" -> font = AppFont.of(`in`.nextString())
                // Un champ inconnu était fatal : la lecture échouait, le fichier
                // partait en .bak et l'utilisateur retrouvait un thème neuf.
                // Ouvrir une version plus récente puis revenir en arrière ne
                // doit pas coûter sa configuration.
                else -> `in`.skipValue()
            }
        }
        `in`?.endObject()
        return Configs(
            taskFieldsOrder = taskFieldsOrders,
            themeColors = themeColors,
            language = language,
            holdKey = holdKey,
            handleY = handleY,
            anchorRight = anchorRight,
            pinned = pinned,
            font = font
        )
    }
}
