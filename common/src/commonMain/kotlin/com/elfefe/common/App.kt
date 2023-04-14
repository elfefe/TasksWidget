package com.elfefe.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.ui.theme.TasksTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


@Composable
fun App(isVisible: (Boolean) -> Unit, isExpanded: (Boolean) -> Unit, changeSide: () -> Unit) {
    val scope = rememberCoroutineScope()

    var showDone by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(true) }

    var tasks by remember { mutableStateOf(listOf<Tasks.Task>()) }

    var expanded by remember { mutableStateOf(true) }
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f)

    Tasks.tasksFlow.onEach {
        println("Update")
        tasks = mutableListOf()
        tasks = it
    }.launchIn(scope)

    TasksTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Color(0x66222222),
                        shape = RoundedCornerShape(5.dp)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable { showDescription = !showDescription }
                            .padding(3.dp),
                        tint = if (showDescription) Color.White else Color.LightGray
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                showDone = !showDone
                                Tasks.filter { if (showDone) true else !it.done }
                            }
                            .padding(3.dp),
                        tint = if (showDone) Color.White else Color.LightGray
                    )
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    Tasks.update(Tasks.Task())
                                    Tasks.refresh()
                                }
                            }
                            .padding(3.dp),
                        tint = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.End) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                expanded = !expanded
                                isExpanded(expanded)
                            }
                            .padding(3.dp)
                            .rotate(expandRotation),
                        tint = Color.White
                    )
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                isVisible(false)
                            }
                            .padding(3.dp),
                        tint = Color.White
                    )
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable(onClick = changeSide)
                            .padding(3.dp),
                        tint = Color.White
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(tasks, key = { it.title }) { task ->
                    TaskCard(scope, task, showDescription)
                }
            }
        }
    }
}

@Composable
fun TaskCard(scope: CoroutineScope, task: Tasks.Task, showDescription: Boolean) {
    var deadline by remember { mutableStateOf(task.deadline) }
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        backgroundColor = Color.White,
        elevation = 5.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val dayMonth = task.deadline.split("/")
                val date = Calendar.getInstance()

                val isDeadlineToday =
                    dayMonth.size == 2 &&
                            date.get(Calendar.DAY_OF_MONTH) == dayMonth[0].toInt() &&
                            date.get(Calendar.MONTH) == dayMonth[1].toInt() - 1

                BasicTextField(
                    value = deadline,
                    onValueChange = {
                        if (it.isBlank()) {
                            deadline = "0"
                            return@BasicTextField
                        }

                        if (it.length <= 4 && it.last().digitToIntOrNull() != null)
                            deadline = it.replace("/", "")

                        if (it.length == 4 && !it.contains("/"))
                            deadline = it.substring(0, 2) + "/" + it.substring(2, 4)

                        scope.launch(Dispatchers.IO) {
                            Tasks.update(task.apply { this.deadline = deadline })
                        }
                    },
                    modifier = Modifier
                        .width(30.dp),
                    textStyle = TextStyle(
                        color = if (isDeadlineToday) Color.Red else Color.DarkGray,
                        fontSize = 10.sp
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(3.dp))

                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        scope.launch(Dispatchers.IO) {
                            Tasks.update(task.apply { this.title = it })
                        }
                    },
                    modifier = Modifier
                        .width(170.dp),
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold),
                    singleLine = true
                )

                Icon(
                    if (task.done) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                Tasks.update(task.apply { done = !done })
                                Tasks.refresh()
                            }
                        },
                    tint = if (task.done) Color.Green else Color.Red
                )
            }
            AnimatedVisibility(
                visible = showDescription,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                ) {
                    BasicTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            scope.launch(Dispatchers.IO) {
                                Tasks.update(task.apply { this.description = it })
                            }
                        }
                    )
                }
            }
        }
    }
}

object Tasks {
    private val path = System.getProperty("user.home") + "\\Documents" + "\\tasks.json"
    private val file = File(path)
    private var currentFilter: (Task) -> Boolean = { true }

    private val _tasksFlow = MutableStateFlow(listOf<Task>())
    val tasksFlow: StateFlow<List<Task>> = _tasksFlow

    init {
        if (file.exists()) {
            _tasksFlow.value = queryTasks()
        }
    }

    private fun queryTasks(): List<Task> {
        return Gson().fromJson(file.readText(), object : TypeToken<List<Task>>() {}.type)
    }

    fun update(task: Task) {
        val tasks = tasksFlow.value.toMutableList()
        val index = tasks.indexOfFirst { it.title == task.title }
        if (index == -1) tasks.add(task)
        else tasks[index] = task
        file.writeText(Gson().toJson(tasks))
    }

    fun filter(filter: (Task) -> Boolean) {
        currentFilter = filter
        refresh()
    }

    fun refresh() {
        _tasksFlow.value = queryTasks().filter(currentFilter)
    }

    class Task(
        var title: String = "",
        var description: String = "",
        var deadline: String = getDate(),
        var done: Boolean = false
    )
}

fun String.toMarkdown(): String {

    return ""
}

fun getDate(): String {
    val date = Calendar.getInstance()
    return date.get(Calendar.DAY_OF_MONTH).toString() + "/" + (date.get(Calendar.MONTH) + 1).toString()
}

