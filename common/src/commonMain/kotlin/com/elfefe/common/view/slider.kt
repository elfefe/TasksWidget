package com.elfefe.common.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import kotlinx.coroutines.launch
import java.awt.MouseInfo
import java.awt.Toolkit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ScrollBar(tasks: List<Task>, listState: LazyListState) {
    val scope = rememberCoroutineScope()

    var sliderPosition by remember { mutableStateOf(0) }
    var sliderHeight by remember { mutableStateOf(0) }
    var sliderHandleHeight by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(8.dp)
            .background(Color.Transparent)
            .onGloballyPositioned { coords ->
                sliderHeight =
                    Toolkit.getDefaultToolkit().screenSize.height - coords.positionInWindow().y.toInt()
            }.pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val position = min(max(MouseInfo.getPointerInfo().location.y, 0), sliderHeight)
                    sliderPosition = min(max(MouseInfo.getPointerInfo().location.y - sliderHandleHeight / 2, 0), sliderHeight - sliderHandleHeight)
                    val index = floor(tasks.size * (position.toFloat() / sliderHeight)).roundToInt()
                    scope.launch { listState.scrollToItem(index) }
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .offset(x = 0.dp, y = sliderPosition.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color = Color.White)
                .height((sliderHeight / tasks.size.toFloat()).dp)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    sliderHandleHeight = coords.size.height
                }
        ) {}
    }
}