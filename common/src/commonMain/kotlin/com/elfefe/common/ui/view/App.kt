package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.TextButton
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
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import com.elfefe.common.model.github.GithubLatestRelease
import com.elfefe.common.ui.theme.TasksTheme
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.awt.Window
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


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
                                "Bearer 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"
                            )
                            .GET()
                            .build(),
                        HttpResponse.BodyHandlers.ofString()
                    )

                    latestRelease = Gson().fromJson(response.body(), GithubLatestRelease::class.java)
                }
            }

            AnimatedVisibility(windowInteractions.expanded and showStatus) {
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
                            newVersionAvailable = tagName != version

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
                            fontSize = 12.sp,
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
                                    latestRelease?.run {
                                        htmlUrl?.let { uriHandler.openUri(it) }
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
    val window: Window,
    val isVisible: (Boolean) -> Unit,
    val isExpanded: (Boolean) -> Unit,
    var expanded: Boolean = false,
    val changeSide: (Boolean, Float) -> Unit,
    val changeHeight: (Float) -> Unit = {},
    val showConfigs: (Boolean) -> Unit
)

data class ToolbarInteractions(
    val showDescription: (Boolean) -> Unit,
)



