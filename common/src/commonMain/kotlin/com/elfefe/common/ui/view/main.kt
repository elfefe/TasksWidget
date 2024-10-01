package com.elfefe.common.ui.view

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.*
import com.elfefe.common.controller.*
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit


fun preload() {
    EmojiApi.preloadEmojis()
}

fun start() {
    preload()
    application { TasksWidget() }
}

@Composable
fun ApplicationScope.TasksWidget() {
    Tasks.scope = rememberCoroutineScope()

    val windowInteractions = WindowInteractions(
        application = this,
        window = Interactable(null),
        visibility = Interactable(true),
        expand = Interactable(true),
        moveWindow = Interactable(WindowMovement()),
        showEmotes = Interactable(false),
        showConfigs = Interactable(false),
        popup = Interactable(Popup())
    )

    TrayWindow(windowInteractions)
    TasksWindow(windowInteractions)
    ConfigsWindow(windowInteractions)
    PopupWindow(windowInteractions)
    EmotesWindow(windowInteractions)

    try {
        generatePowerShellScript()
    } catch (e: Exception) {
        windowInteractions.popup.value = Popup(text = "Failed to generate PowerShell script", duration = 5L)
    }
}

@Composable
fun ApplicationScope.TrayWindow(windowInteractions: WindowInteractions) {
    Tray(
        icon = painterResource("logo-taskswidget-tray.png"),
        tooltip = "Tasks",
        onAction = {
            windowInteractions.visibility.value = true
            windowInteractions.window.value?.requestFocusInWindow()
        },
        menu = {
            Item("Exit", onClick = ::exitApplication)
        },
    )
}

@Composable
fun ApplicationScope.TasksWindow(windowInteractions: WindowInteractions) {
    var isVisible by remember { mutableStateOf(windowInteractions.visibility.value ?: true) }
    var windowExpanded by remember { mutableStateOf(windowInteractions.expand.value ?: true) }

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowMaxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val windowWidth by animateDpAsState(
        if (windowExpanded) kotlin.math.max(256f, windowMaxSize.width * 0.15f).dp else 86.dp,
        tween(
            durationMillis = 500,
            delayMillis = 0,
            easing = EaseInOutCubic
        )
    )
    val windowMargin = 5.dp

    var windowHorizontalMove by remember { mutableStateOf(0.dp) }
    val windowHorizontalPosition by animateDpAsState(
        min(screenSize.width.dp - windowWidth - windowMargin, max(windowMargin, windowHorizontalMove))
    )
    var mousePositionStart = 0.dp
    val windowMinHeight = 29.dp
    val windowHeight by animateDpAsState(
        if (windowExpanded) windowMaxSize.height.dp else windowMinHeight,
        tween(
            durationMillis = 500,
            delayMillis = 0,
            easing = EaseInOutCubic
        )
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            position = WindowPosition(windowHorizontalPosition, 5.dp),
            size = DpSize(windowWidth, windowHeight)
        ),
        visible = isVisible,
        title = "Tasks",
        icon = painterResource("logo-taskswidget.png"),
        transparent = true,
        undecorated = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        windowInteractions.window.value = this.window

        windowInteractions.visibility.onChange = { isVisible = it }
        windowInteractions.expand.onChange = { windowExpanded = it }
        windowInteractions.moveWindow.onChange = { move ->
            if (move.init) mousePositionStart = move.offset.dp
            windowHorizontalMove = MouseInfo.getPointerInfo().location.x.dp - windowWidth + mousePositionStart
        }

        App(windowInteractions)
    }
}

@Composable
fun ApplicationScope.ConfigsWindow(windowInteractions: WindowInteractions) {
    var isConfigsVisible by remember { mutableStateOf(windowInteractions.showConfigs.value ?: false) }
    windowInteractions.showConfigs.onChange = {
        isConfigsVisible = it
    }

    if (isConfigsVisible)
        Window(
            onCloseRequest = {
                windowInteractions.showConfigs.value = false
            },
            state = WindowState(position = WindowPosition(Alignment.Center)),
            title = "Tasks - configs",
            icon = painterResource("logo-taskswidget.png"),
            resizable = true,
            focusable = true
        ) {
            Configs(windowInteractions)
        }
}

@Composable
fun ApplicationScope.PopupWindow(windowInteractions: WindowInteractions) {
    var isPopupVisible by remember { mutableStateOf(windowInteractions.popup.value?.show ?: false) }
    var popupText by remember { mutableStateOf(windowInteractions.popup.value?.text ?: "") }
    val screenSize = Toolkit.getDefaultToolkit().screenSize.run { DpSize(width.dp, height.dp) }
    val timer = Timer(onDone = {
        windowInteractions.popup.value = Popup.HIDE
    })

    windowInteractions.popup.onChange = {
        isPopupVisible = it.show
        popupText = it.text

        if (it.show) {
            timer.cancel()
            timer.start(windowInteractions.popup.value?.duration ?: 3)
        }
    }

    if (isPopupVisible) {
        Window(
            state = WindowState(
                position = WindowPosition(
                    screenSize.width / 4,
                    screenSize.height - 100.dp
                ),
                size = DpSize(
                    screenSize.width / 2, 30.dp * (popupText.count { it == Char(10) } + 1)
                )
            ),
            visible = true,
            onCloseRequest = { windowInteractions.popup.value = Popup.HIDE },
            title = "Tasks - popup",
            icon = painterResource("logo-taskswidget.png"),
            undecorated = true,
            transparent = true,
            resizable = false,
            focusable = false,
            alwaysOnTop = true
        ) {
            Column(
                modifier = Modifier
                    .shadow(8.dp, shape = RoundedCornerShape(4.dp))
                    .fillMaxSize(0.9f)
                    .background(color = Color.White, RoundedCornerShape(4.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { Text(popupText) }
        }
    }
}

@Composable
fun ApplicationScope.EmotesWindow(windowInteractions: WindowInteractions) {
    var isConfigsVisible by remember { mutableStateOf(windowInteractions.showEmotes.value ?: false) }
    windowInteractions.showEmotes.onChange = {
        isConfigsVisible = it
    }

    if (isConfigsVisible)
        Window(
            onCloseRequest = {
                windowInteractions.showEmotes.value = false
            },
            state = WindowState(position = WindowPosition(Alignment.Center)),
            title = "Tasks - Emotes",
            icon = painterResource("logo-taskswidget.png"),
            resizable = true,
            focusable = true
        ) {
            Emotes(windowInteractions)
        }
}