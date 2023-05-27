package com.elfefe.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import javax.swing.filechooser.FileSystemView


@Composable
fun App(windowInteractions: WindowInteractions) {
    val scope = rememberCoroutineScope()

    var tasks by remember { mutableStateOf(listOf<Tasks.Task>()) }

    var showDescription by remember { mutableStateOf(true) }

    Tasks.tasksFlow.onEach {
        tasks = mutableListOf()
        tasks = it
            .sortedByDescending { date -> fromDate(date.deadline) }
            .sortedBy { date -> date.done }
    }.launchIn(scope)

    TasksTheme {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true
            ) {
                items(tasks, key = { it.title }) { task ->
                    TaskCard(Modifier, scope, task, showDescription)
                }
            }
            Toolbar(
                modifier = Modifier
                    .wrapContentHeight(),
                scope = scope,
                windowInteractions = windowInteractions,
                toolbarInteractions = ToolbarInteractions { showDescription = it }
            )
        }
    }
}

data class WindowInteractions(
    val window: Window,
    val isVisible: (Boolean) -> Unit,
    val isExpanded: (Boolean) -> Unit,
    val changeSide: (Boolean, Float) -> Unit,
    val changeHeight: (Float) -> Unit = {},
)

data class ToolbarInteractions(
    val showDescription: (Boolean) -> Unit,
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun Toolbar(
    modifier: Modifier,
    scope: CoroutineScope,
    windowInteractions: WindowInteractions,
    toolbarInteractions: ToolbarInteractions,
) {

    var showDone by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(true) }

    var expanded by remember { mutableStateOf(true) }
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f)

    var showSearch by remember { mutableStateOf(false) }

    var searching by remember { mutableStateOf("") }

    Tasks.filter { if (showDone) true else !it.done }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x66222222),
                shape = RoundedCornerShape(5.dp)
            )
            .then(modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    painter = painterResource("category.svg"),
                    contentDescription = "Category",
                    modifier = Modifier.padding(3.dp),
                    tint = Color.White
                )

                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Description",
                    modifier = Modifier
                        .clickable {
                            showDescription = !showDescription
                            toolbarInteractions.showDescription(showDescription)
                        }
                        .padding(3.dp),
                    tint = if (showDescription) Color.White else Color.LightGray
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = if (showDone) "Hide done" else "Show done",
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
                    contentDescription = "New task",
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    Tasks.update(Tasks.Task())
                                    Tasks.refresh()
                                }
                            },
                            onLongClick = {

                            }
                        )
                        .padding(3.dp),
                    tint = Color.White
                )
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
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
                    contentDescription = if (expanded) "Hide" else "Show",
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
                    contentDescription = "Hide app",
                    modifier = Modifier
                        .clickable {
                            windowInteractions.isVisible(false)
                        }
                        .padding(3.dp),
                    tint = Color.White
                )
                Icon(
                    Icons.Default.Place,
                    contentDescription = "Move app",
                    modifier = Modifier
                        .padding(3.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    windowInteractions.changeSide(true, it.x)
                                }
                            ) { change, dragAmount ->
                                windowInteractions.changeSide(false, 0f)
                            }
                        },
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
fun TaskCard(modifier: Modifier, scope: CoroutineScope, task: Tasks.Task, showDescription: Boolean) {
    var deadline by remember { mutableStateOf(task.deadline) }
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .then(modifier),
        backgroundColor = Color.White,
        elevation = 5.dp
    ) {
        val deadlineDate = deadlineDate(task.deadline)
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
                        .width(IntrinsicSize.Max)
                        .padding(10.dp, 0.dp),
                    textStyle = TextStyle(
                        color =
                        if (task.done || deadlineDate == 1) Color.Black
                        else if (deadlineDate == -1) Color(0xFFFFB900)
                        else Color.Red,
                        fontSize = 10.scaledSp(),
                        fontWeight = FontWeight.SemiBold
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
                        .weight(1f),
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold),
                    singleLine = true
                )

                Icon(
                    if (task.done) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
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
    private val path = FileSystemView.getFileSystemView().defaultDirectory.path + File.separator + "tasks.json"
    private val file = File(path)
    private var currentFilter: (Task) -> Boolean = { true }

    private val _tasksFlow = MutableStateFlow(listOf<Task>())
    val tasksFlow: StateFlow<List<Task>> = _tasksFlow

    init {
        println(path)
        if (file.exists()) {
            _tasksFlow.value = queryTasks()
        } else {
            file.createNewFile()
            file.writeText(Gson().toJson(listOf<Task>()))
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

fun fromDate(date: String): Long {
    val dateOrder = listOf(Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR)
    date.split("/").let {
        return Calendar.getInstance().apply {
            for (i in it.indices) set(dateOrder[i], it[i].toInt())
        }.timeInMillis
    }
}

fun deadlineDate(date: String): Int {
    val dateOrder = listOf(Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR)
    date.split("/").let {
        return Calendar.getInstance().apply {
            for (i in it.indices) set(dateOrder[i], it[i].toInt() - if (i == 1) 1 else 0)
        }.compareTo(Calendar.getInstance())
    }
}

