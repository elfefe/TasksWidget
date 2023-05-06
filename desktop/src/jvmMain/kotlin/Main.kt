import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.elfefe.common.App
import com.elfefe.common.WindowInteractions
import java.awt.Toolkit
import java.awt.Window


fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    var window: Window? = null


    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowWidth = kotlin.math.max(256f, screenSize.width * 0.15f).dp
    val windowMargin = 5.dp

    var isScreenLeft by remember { mutableStateOf(true) }
    val windowHorizontalPosition by animateDpAsState(
        if (isScreenLeft) windowMargin
        else screenSize.width.dp - windowWidth - windowMargin
    )

    var windowExpanded by remember { mutableStateOf(true) }
    val windowHeight by animateDpAsState(if (windowExpanded) 990.dp else 29.dp)

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
                { isScreenLeft = !isScreenLeft }
            )
        )
    }
}
