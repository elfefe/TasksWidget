import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.elfefe.common.controller.ConfigsAutoLoader
import com.elfefe.common.controller.EmojiApi
import com.elfefe.common.controller.EmojiCategory
import com.elfefe.common.view.App
import com.elfefe.common.view.Configs
import com.elfefe.common.view.WindowInteractions
import kotlinx.coroutines.*
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.Window
import javax.management.relation.Role
import kotlin.concurrent.thread


@OptIn(ExperimentalFoundationApi::class)
fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    var isConfigsVisible by remember { mutableStateOf(false) }
    var window: Window? = null

    var windowExpanded by remember { mutableStateOf(true) }

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

    EmojiApi.preloadEmojis()

    ConfigsAutoLoader(rememberCoroutineScope()).launch()

    Tray(
        icon = painterResource("logo-taskswidget-tray.png"),
        tooltip = "Tasks",
        onAction = {
            isVisible = true
            window?.requestFocusInWindow()
        },
        menu = {
            Item("Exit", onClick = ::exitApplication)
        },
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
        window = this.window

        App(
            WindowInteractions(
                this.window,
                { isVisible = it },
                { windowExpanded = it },
                { init, offset ->
                    if (init) mousePositionStart = offset.dp
                    windowHorizontalMove = MouseInfo.getPointerInfo().location.x.dp - windowWidth + mousePositionStart
                },
                showConfigs = {
                    isConfigsVisible = it
                }
            )
        )
    }

    if (isConfigsVisible)
        Window(
            onCloseRequest = {
                isConfigsVisible = false
            },
            title = "Tasks - configs",
            icon = painterResource("logo-taskswidget.png"),
            resizable = true,
            focusable = true
        ) {
            Configs()
        }
}
