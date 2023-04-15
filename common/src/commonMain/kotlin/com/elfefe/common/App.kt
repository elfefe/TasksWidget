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
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import java.awt.Window
import java.io.File
import java.util.*


@Composable
fun App(windowInteractions: WindowInteractions) {
    val scope = rememberCoroutineScope()

    var tasks by remember { mutableStateOf(listOf<Tasks.Task>()) }

    var showDescription by remember { mutableStateOf(true) }

    Tasks.tasksFlow.onEach {
        tasks = mutableListOf()
        tasks = it
    }.launchIn(scope)

    TasksTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Toolbar(scope, windowInteractions, ToolbarInteractions { showDescription = it })
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

data class WindowInteractions(
    val window: Window,
    val isVisible: (Boolean) -> Unit,
    val isExpanded: (Boolean) -> Unit,
    val changeSide: () -> Unit
)

data class ToolbarInteractions(
    val showDescription: (Boolean) -> Unit,
)

@Composable
fun Toolbar(scope: CoroutineScope, windowInteractions: WindowInteractions, toolbarInteractions: ToolbarInteractions) {

    var showDone by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(true) }

    var expanded by remember { mutableStateOf(true) }
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f)

    var showSearch by remember { mutableStateOf(false) }

    var searching by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x66222222),
                shape = RoundedCornerShape(5.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showDescription = !showDescription
                            toolbarInteractions.showDescription(showDescription)
                        }
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
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showSearch = !showSearch
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
                            windowInteractions.isExpanded(expanded)
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
                            windowInteractions.isVisible(false)
                        }
                        .padding(3.dp),
                    tint = Color.White
                )
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable(onClick = windowInteractions.changeSide)
                        .padding(3.dp),
                    tint = Color.White
                )
            }
        }
        AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Color(0xAA111111),
                        shape = RoundedCornerShape(5.dp)
                    )
            ) {
                BasicTextField(
                    value = searching,
                    onValueChange = {
                        searching = it
                        scope.launch {
                            Tasks.filter { task ->
                                task.title.contains(searching, true) ||
                                task.deadline.contains(searching, true) ||
                                task.description.contains(searching, true)
                            }
                        }
                    },
                    textStyle = TextStyle(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 5.dp),
                    cursorBrush = SolidColor(Color.White)
                )
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
                        .padding(10.scaledDp(), 0.dp),
                    textStyle = TextStyle(
                        color = if (isDeadlineToday) Color.Red else Color.DarkGray,
                        fontSize = 10.scaledSp()
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(3.scaledDp()))

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
                modifier = Modifier
                    .fillMaxWidth(),
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
                        },
                        modifier = Modifier
                            .fillMaxSize()
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

@Composable
fun Int.scaledSp(): TextUnit {
    val value: Int = this
    return with(LocalDensity.current) {
        val fontScale = this.fontScale
        val textSize = value / fontScale
        textSize.sp
    }
}

@Composable
fun Int.scaledDp(): Dp {
    val value: Int = this
    return with(LocalDensity.current) {
        val fontScale = this.fontScale
        val textSize = value / fontScale
        textSize.dp
    }
}

fun getDate(): String {
    val date = Calendar.getInstance()
    return date.get(Calendar.DAY_OF_MONTH).toString() + "/" + (date.get(Calendar.MONTH) + 1).toString()
}

