package com.elfefe.common.ui.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
//import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.window.WindowPosition
import com.elfefe.common.controller.*
import kotlinx.coroutines.delay
//import com.google.firebase.FirebaseOptions
//import com.google.firebase.auth.FirebaseAuth
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.util.Calendar
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

val WINDOW_MAX_WIDTH = 256.dp

fun preload() {
    runCatching {
        EmojiApi.preloadEmojis()
    }.onFailure { EmojiApi.log(it.stackTraceToString()) }
}

fun start() {
    System.setProperty("java.util.logging.config.file", logsFile.absolutePath)
    preload()
    application {
        runCatching {
            TasksWidget()
        }.onFailure {
            log(it.stackTraceToString())
        }
    }
}

@Composable
fun ApplicationScope.TasksWidget() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log(e.stackTraceToString())
    }

    runCatching {
        Tasks.scope = rememberCoroutineScope()

        val windowInteractions = WindowInteractions(
            application = this,
            window = Interactable(null),
            visibility = Interactable(true),
            expand = Expanding(true),
            windowSize = Interactable(IntSize(0, 0)),
            moveWindow = WindowMovement(),
            showEmotes = Interactable(false),
            showConfigs = Interactable(false),
            popup = Interactable(Popup())
        )


        TrayWindow(windowInteractions)
        TaskStack(windowInteractions)
        ConfigsWindow(windowInteractions)
        PopupWindow(windowInteractions)
        EmotesWindow(windowInteractions)

        try {
            generatePowerShellScript()
        } catch (e: Exception) {
            windowInteractions.popup.value = Popup(text = "Failed to generate PowerShell script", duration = 5L)
        }
    }.onFailure {
        log(it.stackTraceToString())
    }
}

@Composable
fun ApplicationScope.TrayWindow(windowInteractions: WindowInteractions) {
    Tray(
        icon = painterResource("logo-taskswidget-tray.png"),
        tooltip = "Tasks",
        onAction = {
            windowInteractions.expand.value = true
            windowInteractions.window.value?.requestFocusInWindow()
        },
        menu = {
            Item("Exit", onClick = ::exitApplication)
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApplicationScope.ConfigsWindow(windowInteractions: WindowInteractions) {
    var isConfigsVisible by remember { mutableStateOf(windowInteractions.showConfigs.value ?: false) }
    windowInteractions.showConfigs.addOnChange("ConfigsWindow") {
        isConfigsVisible = it
    }

    if (isConfigsVisible)
        ThemedWindow(
            onCloseRequest = {
                windowInteractions.showConfigs.value = false
            },
            title = "Tasks - configs",
            icon = painterResource("logo-taskswidget.png"),
            resizable = true,
            focusable = true,
            alwaysOnTop = false
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

    windowInteractions.popup.addOnChange("PopupWindow") {
        isPopupVisible = it.show
        popupText = it.text

        if (it.show) {
            timer.cancel()
            timer.start(windowInteractions.popup.value?.duration ?: 3)
        }
    }

    if (isPopupVisible) {
        ThemedWindow(
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
    var isConfigsVisible by remember { mutableStateOf(windowInteractions.showEmotes.value == true) }
    windowInteractions.showEmotes.addOnChange("EmotesWindow") {
        isConfigsVisible = it
    }

    if (isConfigsVisible)
        ThemedWindow(
            onCloseRequest = {
                windowInteractions.showEmotes.value = false
            },
            title = "Tasks - Emotes",
            icon = painterResource("logo-taskswidget.png"),
        ) {
            Emotes(windowInteractions)
        }
}
