import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.*
import com.elfefe.common.App
import com.elfefe.common.WindowInteractions
import java.awt.Toolkit
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    var window: Window? = null

    val icon: BufferedImage = ImageIO.read(File("common/src/commonMain/resources/logo-taskswidget.png"))
    val iconTray: BufferedImage = ImageIO.read(File("common/src/commonMain/resources/logo-taskswidget-tray.png"))


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
        icon = iconTray.toPainter(),
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
        icon = icon.toPainter(),
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
