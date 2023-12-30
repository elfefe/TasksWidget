package com.elfefe.common.controller

import com.elfefe.common.model.Configs
import com.elfefe.common.model.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.filechooser.FileSystemView

object Tasks {
    const val FILE_NAME = "tasks.json"
    const val DIRECTORY = "Tasks"

    val directoryPath = FileSystemView.getFileSystemView().defaultDirectory.path + File.separator + DIRECTORY
    val tasksPath = directoryPath + File.separator + FILE_NAME

    private val file = File(tasksPath)
    var currentFilter: (Task) -> Boolean = { true }

    private val scope = CoroutineScope(Dispatchers.IO)
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
        if (file.exists()) {
            _tasks.clear()
            _tasks.addAll(query())
        } else {
            file.createNewFile()
            file.writeText(json.toJson(listOf<Task>()))
        }
        refresh()
    }

    private fun query(): List<Task> = json.fromJson(file.readText(), object : TypeToken<List<Task>>() {}.type)

    fun update(task: Task) {
        lastTask = task
        waitingTasks.add(task.apply { edited = System.currentTimeMillis() })

        if (updateJob?.isActive == true) return

        updateJob = scope.launch {
            while (waitingTasks.isNotEmpty()) {
                val waitedTask = waitingTasks.poll()
                val index = _tasks.indexOfFirst { it.created == waitedTask.created }

                if (index == -1) _tasks.add(waitedTask)
                else _tasks[index] = waitedTask

                refresh()

                try { file.writeText(json.toJson(_tasks)) }
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
        _tasks.sorting()
        onUpdate(_tasks.filter(currentFilter))
    }

    object Configs {
        const val FILE_NAME = "configs.json"
        val configsPath = directoryPath + File.separator + FILE_NAME
        private val file = File(configsPath)


        private var _configs = Configs()
        val configs: com.elfefe.common.model.Configs = _configs

        private var updateJob: Job? = null
        private val waitingTasks = ConcurrentLinkedQueue<com.elfefe.common.model.Configs>()

        init {
            if (file.exists()) {
                _configs = query()
            } else {
                file.createNewFile()
                file.writeText(json.toJson(_configs))
            }
            refresh()
        }

        fun loadConfigs() {
            updateTasksSort()
        }

        private fun updateTasksSort() {
            sorting = {
                sortWith(
                    compareBy<Task> { it.done }.apply {
                        configs.taskFieldOrders.sortedByDescending { it.priority }.forEach { taskFieldOrder ->
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

        fun update(configs: com.elfefe.common.model.Configs) {
            waitingTasks.add(configs)

            if (updateJob?.isActive == true) return

            updateJob = scope.launch {
                while (waitingTasks.isNotEmpty()) {
                    try {
                        val config = json.toJson(_configs)
                        if (config.isNotBlank())
                        file.writeText(config)
                    }
                    catch (e: Exception) { continue }
                }
                updateJob?.cancelAndJoin()
            }
        }

        private fun query(): com.elfefe.common.model.Configs =
            json.fromJson(file.readText(), object : TypeToken<com.elfefe.common.model.Configs>() {}.type)
    }
}

