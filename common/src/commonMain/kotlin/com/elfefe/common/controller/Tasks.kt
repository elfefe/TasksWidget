package com.elfefe.common.controller

import com.elfefe.common.model.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.filechooser.FileSystemView

object Tasks {
    const val FILE_NAME = "tasks.json"

    val path = FileSystemView.getFileSystemView().defaultDirectory.path + File.separator + FILE_NAME

    private val file = File(path)
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

    init {
        if (file.exists()) {
            _tasks.clear()
            _tasks.addAll(queryTasks())
        } else {
            file.createNewFile()
            file.writeText(json.toJson(listOf<Task>()))
        }
        refresh()
    }

    private fun queryTasks(): List<Task> = json.fromJson(file.readText(), object : TypeToken<List<Task>>() {}.type)

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

    fun MutableList<Task>.sort() =
        sortWith(
            compareByDescending<Task> { it.deadline }
                .thenByDescending { it.created }
                .thenBy { it.done }
        )

    fun refresh() {
        _tasks.sort()
        onUpdate(_tasks.filter(currentFilter))
    }
}