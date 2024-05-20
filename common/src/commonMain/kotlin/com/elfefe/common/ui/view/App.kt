package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import com.elfefe.common.controller.Tasks
import com.elfefe.common.controller.appPrivateDir
import com.elfefe.common.controller.tmpDir
import com.elfefe.common.model.Task
import com.elfefe.common.model.github.GithubLatestRelease
import com.elfefe.common.ui.theme.TasksTheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import java.awt.Window
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.io.path.fileSize


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(windowInteractions: WindowInteractions) {
    val scope = rememberCoroutineScope()

    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var showDescription by remember { mutableStateOf(true) }

    val listState = rememberLazyListState(0)

    Tasks.onUpdate = {
        tasks = it
        scope.launch {
            Tasks.lastTask?.let { task ->
                val index = tasks.indexOf(task)
                listState.animateScrollToItem(if (index < 0) 0 else index)
            }
        }
    }
    Tasks.filter { !it.done }

    TasksTheme {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val uriHandler = LocalUriHandler.current

            var showStatus by remember { mutableStateOf(true) }
            var latestRelease: GithubLatestRelease? by remember { mutableStateOf(null) }

            LaunchedEffect(Unit) {
                scope.launch {
                    val response = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create("https://api.github.com/repos/elfefe/TasksWidget/releases/latest"))
                            .header("Accept", "application/vnd.github.v3+json")
                            .header("X-GitHub-Api-Version", "2022-11-28")
                            .header(
                                "Authorization",
                                "Bearer ghp" + "_93cRkXjvkXwbsjicfo91HMyVCQumAz0U18Sn"
                            )
                            .GET()
                            .build(),
                        HttpResponse.BodyHandlers.ofString()
                    )

                    latestRelease = Gson().fromJson(response.body(), GithubLatestRelease::class.java)
                }
            }

            AnimatedVisibility(windowInteractions.expand.value == true && showStatus) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    var searchingVersion by remember { mutableStateOf("Searching update...") }
                    var newVersionAvailable by remember { mutableStateOf(false) }
                    var shadowColor by remember { mutableStateOf(Color.Black) }

                    latestRelease?.run {
                        useResource("version") {
                            val version = it.readBytes().toString(Charsets.UTF_8)
                            newVersionAvailable = tagName != version && tagName != null

                            if (newVersionAvailable) searchingVersion = "New version available: $tagName"
                            else {
                                searchingVersion = "Up to date"
                                showStatus = false
                            }
                        }
                    }

                    BasicText(
                        text = searchingVersion,
                        style = TextStyle(
                            color = Tasks.Configs.configs.themeColors.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = shadowColor,
                                blurRadius = 2f,
                                offset = Offset.Zero
                            )
                        ),
                        modifier = Modifier
                            .height(20.dp)
                            .padding(4.dp)
                            .onPointerEvent(eventType = PointerEventType.Enter) { shadowColor = Color(0xFF555555) }
                            .onPointerEvent(eventType = PointerEventType.Exit) { shadowColor = Color.Black }
                            .onClick {
                                shadowColor = Color.Black
                                showStatus = false
                                if (newVersionAvailable)
                                    scope.launch(Dispatchers.IO) {
                                        latestRelease?.run {
                                            assets.firstOrNull()?.run {
                                                browserDownloadUrl?.let {
                                                    windowInteractions.popup.value =
                                                        Popup.show("Launched TasksWidget update ! \uD83D\uDE80")
                                                    val updateFile = File(tmpDir, name ?: "TasksWidget.latest.msi")
                                                    println(it)
                                                    val client = HttpClientBuilder.create()
                                                        .setRedirectStrategy(LaxRedirectStrategy())
                                                        .build()
                                                    client.execute(HttpGet(it)).use { response ->
                                                        if (response.statusLine.statusCode == 201) {
                                                            response.entity.content?.use { input ->
                                                                updateFile.outputStream().use { output ->
                                                                    input.copyTo(output)
                                                                }
                                                            }
                                                            ProcessBuilder(
                                                                "msiexec.exe",
                                                                "/i",
                                                                updateFile.absolutePath
                                                            ).start()
                                                            windowInteractions.application.exitApplication()
                                                        } else {
                                                            return@run null
                                                        }
                                                    }
                                                }
                                            } ?: run {
                                                htmlUrl?.let { uriHandler.openUri(it) }
                                                windowInteractions.popup.value =
                                                    Popup(true, "The latest version of TasksWidget could not be updated manually.. \uD83D\uDE1E \n" +
                                                            "Please download and install it manually !", 5)
                                            }
                                        }
                                    }
                            }
                    )
                }
            }
            Toolbar(scope, windowInteractions, ToolbarInteractions { showDescription = it })
            TasksList(tasks, listState, showDescription)
        }
    }
}

data class WindowInteractions(
    val application: ApplicationScope,
    val window: Interactable<Window>,
    val visibility: Interactable<Boolean>,
    val expand: Interactable<Boolean>,
    val moveWindow: Interactable<WindowMovement>,
    val showConfigs: Interactable<Boolean>,
    val popup: Interactable<Popup>
)

class Interactable<T>(value: T? = null, onChange: (T) -> Unit = {}) {

    var value: T? = value
        set(value) {
            field = value
            value?.let {
                onChange(it)
            }
        }
    var onChange: (T) -> Unit = onChange
}

data class ToolbarInteractions(
    val showDescription: (Boolean) -> Unit,
)

data class WindowMovement(val init: Boolean = false, val offset: Float = 0f)
data class Popup(val show: Boolean = false, val text: String = "", val duration: Long = 0) {
    companion object {
        val HIDE = Popup()
        fun show(text: String) = Popup(true, text, 3)
    }
}



