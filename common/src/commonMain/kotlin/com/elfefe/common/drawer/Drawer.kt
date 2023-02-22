package com.elfefe.common.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.WindowState
import java.lang.Float.min

const val CLOSING_SENSITIVITY = 1
const val OPENING_SENSITIVITY = 1

@Composable
fun Drawer(
    drawerContent: @Composable () -> Unit,
    pageContent: @Composable () -> Unit
) {
    var drawerDragged by remember { mutableStateOf(false) }
    var drawerOpened by remember { mutableStateOf(false) }
    val drawerMaxWidth = 0.6f
    var drawerWidth by remember { mutableStateOf(0f) }
    val drawerWidthAnimated by animateFloatAsState(targetValue = drawerWidth)

    val screenWidthPx = WindowState().size.width.value

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Color.Cyan)
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
        ) { pageContent() }

        Row(
            Modifier
                .fillMaxWidth(drawerWidthAnimated)
                .fillMaxHeight()
                .background(Color.Yellow)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            drawerWidth = if (drawerOpened) drawerMaxWidth else 0f
                        }) { change, dragAmount ->
                        drawerWidth = min(change.position.x / screenWidthPx, drawerMaxWidth)
                        drawerOpened = dragAmount >= -CLOSING_SENSITIVITY && drawerWidth > drawerMaxWidth / 2
                    }
                }
        ) { drawerContent() }
    }
}