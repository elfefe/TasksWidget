package com.elfefe.common.controller

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.elfefe.common.model.Configs
import com.elfefe.common.ui.theme.background
import com.elfefe.common.ui.theme.legacyPrimary
import com.elfefe.common.ui.theme.onBackground
import com.elfefe.common.ui.theme.onPrimary
import com.elfefe.common.ui.theme.primary
import com.elfefe.common.model.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.client.network.sockets.mapEngineExceptions
import kotlinx.coroutines.*
import java.io.File
import java.util.Comparator
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.filechooser.FileSystemView

object Tasks {
    var currentFilters: MutableMap<Any, (Task) -> Boolean> = mutableMapOf()

    var scope = CoroutineScope(Dispatchers.IO)
        set(value) {
            field = value
        }
    private var updateJob: Job? = null
    private val waitingTasks = ConcurrentLinkedQueue<Task>()

    private val _tasks = mutableListOf<Task>()
    val tasks: List<Task> = _tasks

    var lastTask: Task? = null
        private set

    var onUpdate: (List<Task>) -> Unit = {}

    private val json: Gson
        get() = GsonBuilder().setPrettyPrinting().create()


    var sorting: MutableList<Task>.() -> Unit = { toMutableList().sortByDescending {
        it.deadline.run { (substring(2, 4) + substring(0, 2)).toInt() }
    } }

    init {
        if (tasksFile.length() > 0) {
            _tasks.clear()
            _tasks.addAll(query())
        } else {
            tasksFile.createNewFile()
            tasksFile.writeText(json.toJson(listOf<Task>()))
        }
        refresh()
    }

    private fun query(): List<Task> = json.fromJson(tasksFile.readText(), object : TypeToken<List<Task>>() {}.type)

    fun update(task: Task) {
        lastTask = task
        waitingTasks.add(task.apply { edited = System.currentTimeMillis() })

        if (updateJob?.isActive == true) return

        updateJob = scope.launch(Dispatchers.IO) {
            while (waitingTasks.isNotEmpty()) {
                val waitedTask = waitingTasks.poll()
                val index = _tasks.indexOfFirst { it.created == waitedTask.created }

                if (index == -1) _tasks.add(waitedTask)
                else _tasks[index] = waitedTask

                refresh()

                try { tasksFile.writeText(json.toJson(_tasks)) }
                catch (e: Exception) { continue }
            }
            updateJob?.cancelAndJoin()
        }
    }

    fun filter(key: Any = "default", filter: (Task) -> Boolean) {
        currentFilters[key] = filter
        refresh()
    }


    fun refresh() {
//        Configs.updateTasksSo rt()
        _tasks.sorting()
        onUpdate(_tasks.filter(
            if (currentFilters.isEmpty()) { { true } }
            else { task -> currentFilters.values.all { it(task) } }
        ))
    }

    object Configs {
        private var _configs = Configs()
            set(value) {
                field = value
                value.onChanged = { update() }
                update()
            }
        val configs: com.elfefe.common.model.Configs
            get() = _configs

        private var updateJob: Job? = null
        private val configsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        init {
            if (configsFile.exists()) {
                _configs = migrate(query())
                println(_configs)
            } else {
                configsFile.createNewFile()
                configsFile.writeText(json.toJson(_configs))
            }
            // Toute modification faite depuis les réglages écrit désormais sur
            // disque : les mutateurs changent l'intérieur de l'objet et non sa
            // référence, si bien que le setter ci-dessus ne les voyait pas.
            _configs.onChanged = { update() }
            refresh()
        }

        /**
         * Rattrape les thèmes restés sur les anciennes couleurs livrées. Comme
         * les modifications n'ont jamais été enregistrées jusqu'ici, un fichier
         * portant exactement l'ancien jeu de couleurs signale un thème que
         * personne n'a choisi : il suit la nouvelle palette, plus contrastée.
         * Un thème réellement personnalisé, lui, n'est pas touché.
         */
        private fun migrate(loaded: com.elfefe.common.model.Configs): com.elfefe.common.model.Configs {
            val colors = loaded.themeColors
            val untouched = colors.primary == Color.legacyPrimary &&
                    colors.onPrimary == Color.onPrimary &&
                    colors.background == Color.background &&
                    colors.onBackground == Color.onBackground
            if (untouched) loaded.updateThemeColors(primary = Color.primary)
            return loaded
        }

        fun updateTasksSort() {
            sorting = {
                sortWith(
                    compareBy<Task> { it.done }.apply {
                        configs.taskFieldsOrder.sortedByDescending { it.priority }.forEach { taskFieldOrder ->
                            when (taskFieldOrder.name) {
                                "title" ->
                                    if (taskFieldOrder.active)
                                        if (taskFieldOrder.priority > 0) thenByDescending { it.title }
                                        else  thenBy { it.title }
                                "description" ->
                                    if (taskFieldOrder.active)
                                        if (taskFieldOrder.priority > 0) thenByDescending { it.description }
                                        else  thenBy { it.description }
                                "deadline" ->
                                    if (taskFieldOrder.active)
                                        if (taskFieldOrder.priority > 0) thenByDescending { it.deadline }
                                        else  thenBy { it.deadline }
                                "done" ->
                                    if (taskFieldOrder.active)
                                        if (taskFieldOrder.priority > 0) thenByDescending { it.done }
                                        else  thenBy { it.done }
                                "created" ->
                                    if (taskFieldOrder.active)
                                        if (taskFieldOrder.priority > 0) thenByDescending { it.created }
                                        else  thenBy { it.created }
                                "edited" ->
                                    if (taskFieldOrder.active)
                                        if (taskFieldOrder.priority > 0) thenByDescending { it.edited }
                                        else  thenBy { it.edited }
                            }
                        }
                    }
                )
            }
        }

        /**
         * Écrit la configuration après une courte accalmie.
         *
         * L'ancienne version renonçait purement et simplement quand une
         * écriture était en cours (`if (updateJob?.isActive) return`) : en
         * glissant un curseur de couleur, seule la toute première valeur
         * partait sur disque et l'état final était perdu. On annule plutôt
         * l'écriture en attente et on réécrit l'état courant.
         *
         * Le scope est propre à la configuration : `Tasks.scope` est remplacé
         * par celui d'un composable, qui meurt avec lui et emporterait les
         * sauvegardes suivantes.
         */
        fun update() {
            updateJob?.cancel()
            updateJob = configsScope.launch {
                delay(200)
                runCatching {
                    val config = json.toJson(configs)
                    if (config.isNotBlank()) configsFile.writeText(config)
                }.onFailure { log("Configs: enregistrement impossible\n" + it.stackTraceToString()) }
            }
        }

        private fun query(): com.elfefe.common.model.Configs {
            val configsText = configsFile.readText()
            return if (configsText.isBlank()) Configs()
            else try {
                return json.fromJson(configsText, object : TypeToken<com.elfefe.common.model.Configs>() {}.type)
            } catch (e: Exception) {
                File("${configsFile.parent}/tmp").let {
                    if (!it.exists()) it.mkdirs()
                    val name = "${configsFile.name}.${Date().time}.bak"
                    configsFile.renameTo(File(if (!it.exists()) configsFile.parent else it.absolutePath, name))
                }
                return Configs()
            }
        }
    }
}

