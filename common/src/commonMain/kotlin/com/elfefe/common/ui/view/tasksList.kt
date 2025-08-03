package com.elfefe.common.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.elfefe.common.model.Task

@Composable
fun ColumnScope.TasksList(tasks: List<Task>, windowInteractions: WindowInteractions, listState: LazyListState, showDescription: Boolean) {
    Box(
        modifier = Modifier
//            .background(Color.Black)
            .fillMaxWidth()
    ) {
//        Column(
//            modifier = Modifier
//                .graphicsLayer(alpha = 0f)
//                .height(IntrinsicSize.Min)
//                .onGloballyPositioned { coordinates ->
//                    windowInteractions.windowSize.value = coordinates.size.run { IntSize(width, height) }
//                }
//        ) {
//            tasks.forEach { task ->
//                TaskCard(Modifier.height(IntrinsicSize.Min), task, windowInteractions, showDescription)
//            }
//        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = listState
        ) {
            items(tasks, key = { it.created }) { task ->
                TaskCard(Modifier, task, windowInteractions, showDescription)
            }
        }
    }
}