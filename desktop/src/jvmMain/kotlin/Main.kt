import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.elfefe.common.App
import java.awt.FileDialog

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
