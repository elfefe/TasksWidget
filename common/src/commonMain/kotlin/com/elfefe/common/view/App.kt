package com.elfefe.common.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.Tasks
import com.elfefe.common.controller.deadlineDate
import com.elfefe.common.model.Task
import com.elfefe.common.ui.theme.TasksTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.Window
import javax.swing.SwingUtilities
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


@Composable
fun App(windowInteractions: WindowInteractions) {
    val scope = rememberCoroutineScope()

    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var showDescription by remember { mutableStateOf(true) }

    val listState = rememberLazyListState(0)

    Tasks.onUpdate =  {
        tasks = it
        scope.launch {
            Tasks.lastTask?.let { task ->
                val index = tasks.indexOf(task)
                listState.animateScrollToItem(if (index < 0) 0 else index)
            }
        }
    }
    Tasks.filter { !it.done }

    TasksTheme {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Toolbar(scope, windowInteractions, ToolbarInteractions { showDescription = it })
            TasksList(tasks, listState, showDescription)
        }
    }
}

data class WindowInteractions(
    val window: Window,
    val isVisible: (Boolean) -> Unit,
    val isExpanded: (Boolean) -> Unit,
    val changeSide: (Boolean, Float) -> Unit,
    val changeHeight: (Float) -> Unit = {},
    val showConfigs: (Boolean) -> Unit
)

data class ToolbarInteractions(
    val showDescription: (Boolean) -> Unit,
)



@Composable
fun Int.scaledSp(): TextUnit {
    val value: Int = this
    return with(LocalDensity.current) {
        val fontScale = this.fontScale
        val textSize = value / fontScale
        textSize.sp
    }
}



