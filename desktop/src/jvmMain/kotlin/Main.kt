import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.*
import com.elfefe.common.App
import com.elfefe.common.WindowInteractions
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.Window
import java.io.File
import java.nio.file.Paths


fun main() = application {
    Thread.setDefaultUncaughtExceptionHandler { t, e ->

    }

    var isVisible by remember { mutableStateOf(true) }
    var window: Window? = null


    val screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val windowWidth = kotlin.math.max(256f, screenSize.width * 0.15f).dp
    val windowMargin = 5.dp

    var windowHorizontalMove by remember { mutableStateOf(0.dp) }
    val windowHorizontalPosition by animateDpAsState(
        min(screenSize.width.dp - windowWidth - windowMargin, max(windowMargin, windowHorizontalMove))
    )

    val windowVerticalHeight = screenSize.height.dp - windowMargin

    var mousePositionStart = 0.dp

    var windowExpanded by remember { mutableStateOf(true) }
    val windowHeight by animateDpAsState(
        if (windowExpanded) windowVerticalHeight - windowMargin else 30.dp
    )

    val windowVerticalPosition by animateDpAsState(
        if (windowExpanded) windowMargin else windowVerticalHeight - 30.dp
    )

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
            position = WindowPosition(windowHorizontalPosition, if (windowExpanded) windowMargin else windowVerticalHeight - 30.dp),
            size = DpSize(windowWidth, if (windowExpanded) windowVerticalHeight - windowMargin else 30.dp)
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
                }
            )
        )
    }
}
