import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.Window
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.*
import com.elfefe.common.App
import com.elfefe.common.WindowInteractions
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.Window


fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    var window: Window? = null


    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowWidth = kotlin.math.max(256f, screenSize.width * 0.15f).dp
    val windowMargin = 5.dp

    var windowHorizontalMove by remember { mutableStateOf(0.dp) }
    val windowHorizontalPosition by animateDpAsState(
        min(screenSize.width.dp - windowWidth - windowMargin, max(windowMargin, windowHorizontalMove))
    )
    var mousePositionStart = 0.dp

    var windowExpanded by remember { mutableStateOf(true) }
    val windowHeight by animateDpAsState(
        if (windowExpanded) 990.dp else 29.dp,
        tween(
            durationMillis = 500,
            delayMillis = 0,
            easing = EaseInOutCubic
        )
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
                }
            )
        )
    }
}
