package fr.exem.common.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.Float.min

const val CLOSING_SENSITIVITY = 2
const val OPENING_SENSITIVITY = 2

@Composable
fun Drawer(
    drawerContent: @Composable () -> Unit,
    pageContent: @Composable (Modifier) -> Unit
) {
    var drawerDragged by remember { mutableStateOf(false) }
    var drawerOpened by remember { mutableStateOf(false) }
    val drawerMaxWidth = 0.4f
    var drawerWidth by remember { mutableStateOf(0f) }
    val drawerWidthAnimated by animateFloatAsState(targetValue = drawerWidth)

    val screenWidthPx = WindowState().size.width.value

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            pageContent(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { position ->
                                drawerDragged = position.x < screenWidthPx / 2
                            },
                            onDragEnd = {
                                drawerWidth = if (drawerOpened) drawerMaxWidth else 0f
                            }) { change, dragAmount ->
                            if (drawerDragged) {
                                drawerWidth = min(change.position.x / screenWidthPx, drawerMaxWidth)
                                drawerOpened = dragAmount > OPENING_SENSITIVITY || drawerWidth > drawerMaxWidth / 2
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { drawerWidth = 0f }
                    }
            )
        }

        Row(
            Modifier
                .offset (((-drawerMaxWidth * screenWidthPx) + (drawerWidthAnimated * screenWidthPx)).dp, 0.dp)
                .fillMaxWidth(drawerMaxWidth)
                .fillMaxHeight()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            drawerWidth = if (drawerOpened) drawerMaxWidth else 0f
                        }) { change, dragAmount ->
                        drawerWidth = min(change.position.x / screenWidthPx, drawerMaxWidth)
                        drawerOpened = dragAmount >= -CLOSING_SENSITIVITY && drawerWidth > drawerMaxWidth / 2
                    }
                }
                .shadow(50.dp)
        ) { drawerContent() }
    }
}