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

//        throw RuntimeException("Test exception") // Uncomment to test error handling

        /* FirebaseOptions.Builder()
             .setCredentials(FirestoreApi.instance.credentials)
             .setProjectId("taskwidget-b17c3")
             .build()
         FirebaseAuth
             .getInstance()
             .signInWithEmailAndPassword("f.bou-reiff@orange.fr", "***REMOVED-SECRET***")
             .addOnCompleteListener {
                 if (it.isSuccessful) {
                     log("Connected to Firebase")
                 } else {
                     log("Failed to connect to Firebase")
                 }
             }

         OAuthApi(CoroutineScope(Dispatchers.IO)).auth(
             clientId = "1086878445333-tgnhihe3rkaigfqs39umarbfsptb1lr5.apps.googleusercontent.com",
             clientSecret = "***SECRET-PURGE-2026-07-27***",
         ) { token, payload ->
             log("Connected with\n$token\n$payload")
             FirestoreApi.instance.analytics(AccessToken.newBuilder().apply { tokenValue = token.accessToken }.build())
         }*/

        TrayWindow(windowInteractions)
        TasksWindow(windowInteractions)
        HandleWindow(windowInteractions)
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

//@OptIn(ExperimentalAnimationSpecApi::class)
@Composable
fun ApplicationScope.TasksWindow(windowInteractions: WindowInteractions) {
    var isVisible by remember { mutableStateOf(windowInteractions.visibility.value ?: true) }
    var windowExpanded by remember { mutableStateOf(windowInteractions.expand.value ?: true) }

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowMaxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    var windowMinSize by remember {
        mutableStateOf(windowMaxSize.run { IntSize(width, height) })
    }
    val windowMargin = 0.dp

    var windowHorizontalMove by remember { mutableStateOf(windowMaxSize.width.dp) }

    val targetX by animateDpAsState(
        if (windowHorizontalMove > screenSize.width.dp / 2) screenSize.width.dp - WINDOW_MAX_WIDTH else 0.dp
    )

    val alphaAnimation = remember { Animatable(0f) }
    val positionAnimation = remember { Animatable(0f) }

    LaunchedEffect(windowHorizontalMove > screenSize.width.dp / 2) {
        alphaAnimation.animateTo(0f, animationSpec = tween(100))
        alphaAnimation.animateTo(1f, animationSpec = tween(500))
    }

    LaunchedEffect(windowHorizontalMove > screenSize.width.dp / 2) {
        windowInteractions.moveWindow.isRight.value = windowHorizontalMove > screenSize.width.dp / 2
        positionAnimation.animateTo(if (windowHorizontalMove > screenSize.width.dp / 2) screenSize.width - WINDOW_MAX_WIDTH.value else 0f, animationSpec = tween(400))
    }

    var mousePositionStart = 0.dp
    val windowMinHeight = 29.dp
    val windowHeight by animateDpAsState(
        if (windowExpanded) windowMinSize.height.dp else windowMinHeight,
        tween(
            durationMillis = 500,
            delayMillis = 0,
            easing = EaseInOutCubic
        )
    )
    val windowOffset = remember { Animatable(0f) }

    LaunchedEffect("$windowExpanded${positionAnimation.value}") {
        windowOffset.animateTo(
            if (windowExpanded) 0f
            else if (positionAnimation.value > 0) WINDOW_MAX_WIDTH.value
            else -WINDOW_MAX_WIDTH.value,
            tween(
                durationMillis = 500,
                delayMillis = if (windowExpanded) 0 else 0,
                easing = EaseInOutCubic
            ))
    }

    LaunchedEffect(abs(windowOffset.value).toInt()) {
        val absOffsetValue =
            if (windowExpanded) floor(abs(windowOffset.value))
            else ceil(abs(windowOffset.value))
        if (absOffsetValue == WINDOW_MAX_WIDTH.value) {
            windowInteractions.expand.animate(Expanding.State.Collapsed)
        } else if (absOffsetValue == 0f) {
            windowInteractions.expand.animate(Expanding.State.Expanded)
        }
    }

//    windowInteractions.windowSize.onChange = {
//        println("$it | $windowExpanded | $isVisible")
//        if (it.height == 0 || !windowExpanded || !isVisible) windowMinSize = windowMaxSize.run { IntSize(width, height) }
//        windowMinSize = IntSize(windowMinSize.width, it.height + windowMinHeight.value.toInt())
//    }

    ThemedWindow(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition(positionAnimation.value.dp, 0.dp),
            size = DpSize(WINDOW_MAX_WIDTH, windowMinSize.height.dp)
        ),
        visible = abs(windowOffset.value) != WINDOW_MAX_WIDTH.value,
        title = "Tasks",
        icon = painterResource("logo-taskswidget.png"),
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        windowInteractions.window.value = this.window

        windowInteractions.visibility.addOnChange("TasksWindow") { isVisible = it }
        windowInteractions.expand.addOnChange("TasksWindow") { windowExpanded = it }
        windowInteractions.moveWindow.addOnChange("TasksWindow") { move ->
            windowHorizontalMove = MouseInfo.getPointerInfo().location.x.dp - windowInteractions.moveWindow.origin.dp
        }
        /**/
        App(
            Modifier
                .alpha(alphaAnimation.value)
                .offset(windowOffset.value.dp, 0.dp)
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            println(event.type)
                            when (event.type) {
                                PointerEventType.Exit, PointerEventType.Release -> {
                                    if (!windowInteractions.moveWindow.isActive) {
                                        windowInteractions.expand.value = false
                                    }
                                }
                                PointerEventType.Enter, PointerEventType.Move -> {
                                    if (!windowInteractions.moveWindow.isActive) {
                                        windowInteractions.expand.value = true
                                    }
                                }
                            }
                        }
                    }
                },
            windowInteractions
        )
    }
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

@Composable
fun ApplicationScope.HandleWindow(windowInteractions: WindowInteractions) {
    var tasksWindow: Window? by remember { mutableStateOf(windowInteractions.window.value) }
    val screenSize = Toolkit.getDefaultToolkit().screenSize.run { DpSize(width.dp, height.dp) }
    val windowSize = Size(20f, 50f)
    var windowPosition by remember {
        mutableStateOf(
            WindowPosition(
                x = screenSize.width / 2,
                y = screenSize.height / 2
            )
        )
    }
    var isVisible by remember { mutableStateOf(true) }
    var isActive by remember { mutableStateOf(true) }
    var taskState: Expanding.State by remember { mutableStateOf(Expanding.State.Expanded) }
    var isHovered by remember { mutableStateOf(false) }
    val windowOffset = remember { Animatable(0f) }

    var lastLocation by remember { mutableStateOf(Point(0, 0)) }

    val handleVisibility = remember { Animatable(1f) }
    val shadowRadius by animateFloatAsState(targetValue = if (isHovered) 1f else 2f)

    var taskIsActive by remember { mutableStateOf(true) }

    val handleSize by animateSizeAsState(
        targetValue =
            if (isHovered)
                Size(
                    width = windowSize.width * 0.2f,
                    height = windowSize.height * 0.9f
                )
            else Size(
                width = windowSize.width * 0.4f,
                height = windowSize.width * 0.4f
            ),
    )

    LaunchedEffect(taskState) {
        windowOffset.animateTo(
                if (taskState == Expanding.State.Collapsed) 0f else if ((tasksWindow?.x?.toFloat()
                        ?: 0f) > screenSize.width.value / 2f
                ) -WINDOW_MAX_WIDTH.value else WINDOW_MAX_WIDTH.value,
        tween(200)
        )
    }

    LaunchedEffect("$isVisible$taskState") {
        handleVisibility.animateTo(if (isVisible) 1f else 0f, tween(200))
    }

    windowInteractions.window.addOnChange("HandleWindow") { window ->
        tasksWindow = window
        windowPosition = if (window.x > screenSize.width / 2.dp)
            WindowPosition(window.x.dp + window.width.dp - windowSize.width.dp, screenSize.height / 2)
        else WindowPosition(window.x.dp, screenSize.height / 2)
    }

    windowInteractions.moveWindow.addOnChange("HandleWindow") { movement ->
        tasksWindow?.let { window ->
            windowPosition = if (window.x > screenSize.width / 2.dp)
                WindowPosition(window.x.dp + window.width.dp - windowSize.width.dp, screenSize.height / 2)
            else WindowPosition(window.x.dp, screenSize.height / 2)
        }
    }

    windowInteractions.expand.onAnimateChange("HandleWindow") {
        taskIsActive = it == Expanding.State.Expanding || it == Expanding.State.Collapsing
        taskState = it
    }

    ThemedWindow(
        onCloseRequest = {},
        title = "Tasks - Emotes",
        icon = painterResource("logo-taskswidget.png"),
        resizable = false,
        focusable = true,
        alwaysOnTop = true,
        visible = taskState != Expanding.State.Expanded,
        state = WindowState(
            position = WindowPosition(windowPosition.x + windowOffset.value.dp, windowPosition.y),
            size = DpSize(
                width = windowSize.width.dp,
                height = windowSize.height.dp
            )
        )
    ) {
        fixedRateTimer("mouseCheck", initialDelay = 0, period = 50) {
            if (this@ThemedWindow.window.isShowing)
                lastLocation = this@ThemedWindow.window.locationOnScreen
            val size = this@ThemedWindow.window.size
            isHovered =
                !taskIsActive &&
                isMouseNearWindow(lastLocation, size, 64)
            isVisible =
                isMouseNearWindow(lastLocation, size, 256)
                && taskState == Expanding.State.Collapsed
        }

        Canvas(
            modifier = Modifier
                .alpha(handleVisibility.value)
                .graphicsLayer(alpha = 0.99f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Release -> {
                                    windowInteractions.expand.value = true
                                }
                            }
                        }
                    }
                }
        ) {
            val half = handleSize
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val rectTopLeft = Offset(
                x = size.width - half.width - 2,
                y = size.height / 2 - half.height / 2
            )
            val rectCenter = Offset(
                x = rectTopLeft.x + half.width / 2,
                y = rectTopLeft.y + half.height / 2
            )

            val shadowWidth = half.width + shadowRadius * 2
            val shadowHeight = half.height + shadowRadius * 2
            val gradientRadius =
                sqrt((shadowWidth / 2) * (shadowWidth / 2) + (shadowHeight / 2) * (shadowHeight / 2))

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                    center = rectCenter,
                    radius = gradientRadius
                ),
                topLeft = Offset(rectTopLeft.x - shadowRadius, rectTopLeft.y - shadowRadius),
                size = Size(shadowWidth, shadowHeight),
                cornerRadius = CornerRadius(cornerRadius.x + shadowRadius, cornerRadius.y + shadowRadius)
            )

            drawRoundRect(
                color = Color.Transparent,
                topLeft = rectTopLeft,
                size = half,
                cornerRadius = cornerRadius,
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = Color.White,
                topLeft = rectTopLeft,
                size = half,
                cornerRadius = cornerRadius
            )
        }
    }
}
