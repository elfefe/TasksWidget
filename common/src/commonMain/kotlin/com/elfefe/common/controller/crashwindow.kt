package com.elfefe.common.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.awt.MouseInfo
import java.awt.Toolkit

@Composable
fun ThemedWindow(
    onCloseRequest: () -> Unit,
    state: WindowState? = null,
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    backgroundColor: Color = Color.Transparent,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit
) {
    var windowPosition by remember { mutableStateOf(WindowPosition.Absolute(
        (Toolkit.getDefaultToolkit().screenSize.width / 2 - 300).dp,
        (Toolkit.getDefaultToolkit().screenSize.height / 2 - 300).dp
    )) }
    var windowSize by remember { mutableStateOf(DpSize(800.dp, 600.dp)) }
    var mousePositionOffset by remember { mutableStateOf(Offset.Zero) }

    CrashWindow(
        onCloseRequest = onCloseRequest,
        state = state ?: WindowState(position = windowPosition, size = windowSize),
        visible = visible,
        title = title,
        icon = icon,
        undecorated = true,
        transparent = true,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent
    ) {
        Surface(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        mousePositionOffset = MouseInfo.getPointerInfo().location
                            .run { Offset(
                                x - windowPosition.x.toPx(),
                                y - windowPosition.y.toPx()
                            ) }
                    },
                    onDrag = { _, _ ->
                        MouseInfo.getPointerInfo().location.run {
                            windowPosition = WindowPosition.Absolute(
                                (x - mousePositionOffset.x).dp,
                                (y - mousePositionOffset.y).dp
                            )
                        }
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            },
            color = Color.Transparent
        ) { content() }
    }
}

@Composable
fun CrashWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit
) {
    runCatching {
        Window(
            onCloseRequest = onCloseRequest,
            state = state,
            visible = visible,
            title = title,
            icon = icon,
            undecorated = undecorated,
            transparent = transparent,
            resizable = resizable,
            enabled = enabled,
            focusable = focusable,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
        ) {
            runCatching { content() }.run {
                if (isFailure) exceptionOrNull()
                    ?.stackTraceToString()?.let(::log)
            }
        }
    }.run {
        if (isFailure) exceptionOrNull()?.stackTraceToString()?.let(::log)
    }
}