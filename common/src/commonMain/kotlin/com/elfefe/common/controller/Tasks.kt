package com.elfefe.common.controller

import androidx.compose.ui.graphics.toArgb
import com.elfefe.common.model.Configs
import com.elfefe.common.model.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.filechooser.FileSystemView

object Tasks {
    var currentFilter: (Task) -> Boolean = { true }

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


    var sorting: MutableList<Task>.() -> Unit = { sortByDescending { it.deadline } }

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

    fun filter(filter: (Task) -> Boolean) {
        currentFilter = filter
        refresh()
    }


    fun refresh() {
//        Configs.updateTasksSo rt()
        _tasks.sorting()
        onUpdate(_tasks.filter(currentFilter))
    }

    object Configs {
        private var _configs = Configs()
            set(value) {
                field = value
                update()
            }
        val configs: com.elfefe.common.model.Configs
            get() = _configs

        private var updateJob: Job? = null

        init {
            if (configsFile.exists()) {
                _configs = query()
                println(_configs)
            } else {
                configsFile.createNewFile()
                configsFile.writeText(json.toJson(_configs))
            }
            refresh()
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

        fun update() {
            if (updateJob?.isActive == true) return

            updateJob = scope.launch(Dispatchers.IO) {
                try {
                    val config = json.toJson(configs)
                    if (config.isNotBlank())
                        configsFile.writeText(config)
                } catch (e: Exception) {  }
                updateJob?.cancelAndJoin()
            }
        }

        private fun query(): com.elfefe.common.model.Configs {
            val configsText = configsFile.readText()
            return if (configsText.isBlank()) Configs()
            else json.fromJson(configsText, object : TypeToken<com.elfefe.common.model.Configs>() {}.type)
        }
    }
}

