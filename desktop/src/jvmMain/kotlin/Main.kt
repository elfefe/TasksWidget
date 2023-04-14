import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.elfefe.common.App
import java.awt.Toolkit
import java.awt.Window

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    var window: Window? = null
    val image = Icons.Default.List

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowWidth = (screenSize.width * 0.15f).dp
    val windowMargin = 5.dp

    var isScreenLeft by remember { mutableStateOf(true) }
    val windowHorizontalPosition by animateDpAsState(
        if (isScreenLeft) windowMargin
        else screenSize.width.dp - windowWidth - windowMargin
    )

    var windowExpanded by remember { mutableStateOf(true) }
    val windowHeight by animateDpAsState(if (windowExpanded) 990.dp else 29.dp)

    Tray(
        icon = rememberVectorPainter(
            defaultWidth = image.defaultWidth,
            defaultHeight = image.defaultHeight,
            viewportWidth = image.viewportWidth,
            viewportHeight = image.viewportHeight,
            name = image.name,
            tintColor = Color(0xFFFFBF00),
            tintBlendMode = image.tintBlendMode,
            autoMirror = image.autoMirror,
            content = { _, _ -> RenderVectorGroup(group = image.root) }
        ),
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
        transparent = true,
        undecorated = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        window = this.window
        App(
            { isVisible = it },
            { windowExpanded = it },
            { isScreenLeft = !isScreenLeft }
        )
    }
}
