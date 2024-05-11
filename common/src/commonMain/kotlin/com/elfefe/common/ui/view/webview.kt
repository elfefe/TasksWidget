package com.elfefe.common.ui.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.io.File
import kotlin.math.max


@Composable
fun ApplicationScope.Tests() {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            position = WindowPosition(0.dp, 5.dp),
            size = Toolkit.getDefaultToolkit().screenSize.run { DpSize(width.dp / 2, height.dp) }
        ),
        visible = true,
        title = "Tasks",
        icon = painterResource("logo-taskswidget.png"),
        transparent = true,
        undecorated = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        var restartRequired by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(0F) }
        var initialized by remember { mutableStateOf(false) }
        val webViewSCope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            webViewSCope.launch(Dispatchers.IO) {
                KCEF.init(builder = {
                    installDir(File("kcef-bundle"))
                    progress {
                        onDownloading {
                            downloading = max(it, 0F)
                        }
                        onInitialized {
                            initialized = true
                        }
                    }
                    settings {
                        cachePath = File("cache").absolutePath
                    }
                }, onError = {
                    it?.printStackTrace()
                }, onRestartRequired = {
                    restartRequired = true
                })
            }
        }

        if (restartRequired) {
            Text(text = "Restart required.")
        } else {
            if (initialized) {
                val state = rememberWebViewState("https://www.google.com")
                WebView(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(text = "Downloading $downloading%")
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                KCEF.disposeBlocking()
            }
        }
    }
}