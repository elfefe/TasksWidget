package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import kotlinx.coroutines.CoroutineScope
import java.awt.Desktop
import java.net.URI
import java.util.Locale


@Composable
fun ColumnScope.Toolbar(
    scope: CoroutineScope,
    windowInteractions: WindowInteractions,
    toolbarInteractions: ToolbarInteractions
) {

    var showConfigs by remember { mutableStateOf(false) }

    var showDone by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(true) }

    var expanded by remember { mutableStateOf(true) }
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f)

    var showSearch by remember { mutableStateOf(false) }

    var searching by remember { mutableStateOf("") }

    // Menu du « + » : choisir entre une tâche normale et une session Claude.
    var showAddMenu by remember { mutableStateOf(false) }
    var showClaudeLaunch by remember { mutableStateOf(false) }

    val colors = Tasks.Configs.configs.themeColors

    windowInteractions.expand.addOnChange("Toolbar") { expanded = it }

    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        windowInteractions.moveWindow.isActive = true
                        windowInteractions.moveWindow.origin = it.x
                    },
                    onDragEnd = {
                        windowInteractions.moveWindow.isActive = false
                        windowInteractions.moveWindow.origin = 0f
                    },
                    onDragCancel = {
                        windowInteractions.moveWindow.isActive = false
                        windowInteractions.moveWindow.origin = 0f
                    }
                ) { change, dragAmount ->
                    windowInteractions.moveWindow.value = change.position.x
                }
            }
            .fillMaxWidth()
            .background(
                color = Tasks.Configs.configs.themeColors.primary,
                shape = RoundedCornerShape(5.dp)
            )
    ) {
        val composables = listOf<@Composable () -> Unit>(
            {
                Icon(
                    painterResource(if (!showDescription) "baseline_notes_24.svg" else "short_text_24px.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showDescription = !showDescription
                            toolbarInteractions.showDescription(showDescription)
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                Icon(
                    painterResource(if (!showDone) "check_circle_24px.svg" else "unpublished_24px.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showDone = !showDone
                            Tasks.filter("show done") {
                                if (showDone) true else !it.done
                            }
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary.run {
                        if (showDone) copy(alpha = .6f) else this
                    }
                )
            },
            {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showAddMenu = !showAddMenu
                            if (!showAddMenu) showClaudeLaunch = false
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showSearch = !showSearch
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            /*Icon(
                painterResource("login.svg"),
                contentDescription = null,
                modifier = Modifier
                    .padding(3.dp)
                    .clickable {
                        FirestoreApi.instance.connectTasks(
                            User("felion33@gmail.com", "Félix", "", mutableListOf())
                        ) {
                            println(it)
                        }
                    OAuthApi(scope).apply {
                        auth(
                            "1086878445333-tgnhihe3rkaigfqs39umarbfsptb1lr5.apps.googleusercontent.com",
                            "***SECRET-PURGE-2026-07-27***"
                        ) { jwToken, payload ->
                            println(jwToken)
                            println(payload)
                            val credentials = GoogleCredentials
                                .create(AccessToken(jwToken.accessToken, Date(Date().time + jwToken.expiresIn.toLong())))
                                .createScoped(
                                    "https://www.googleapis.com/auth/cloud-platform"
                                )
                        }
                    }
                    },
                tint = Color.White
            )*/
            {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable {
                            showConfigs = !(windowInteractions.showConfigs.value ?: false)
                            windowInteractions.showConfigs.value = showConfigs
                        },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable {
                            fun openInBrowser(uri: URI) {
                                val osName by lazy(LazyThreadSafetyMode.NONE) { System.getProperty("os.name").lowercase(
                                    Locale.getDefault()) }
                                val desktop = Desktop.getDesktop()
                                when {
                                    Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> desktop.browse(uri)
                                    "mac" in osName -> Runtime.getRuntime().exec("open $uri")
                                    "nix" in osName || "nux" in osName -> Runtime.getRuntime().exec("xdg-open $uri")
                                    else -> throw RuntimeException("cannot open $uri")
                                }
                            }

                            openInBrowser(URI("https://domain.tld/page"))
                        },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            })
        Row(
            modifier = Modifier
                .padding(8.dp, 0.dp)
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = if (windowInteractions.moveWindow.isRight.value) Arrangement.End else Arrangement.Start,
        ) {
            val parameters = if (windowInteractions.moveWindow.isRight.value) composables else composables.reversed()
            parameters.forEach { it() }
        }

//            Row(horizontalArrangement = Arrangement.End) {
//                Icon(
//                    Icons.Default.ArrowDropDown,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .clickable {
//                            windowInteractions.expand.value = !expanded
//                        }
//                        .padding(3.dp)
//                        .rotate(expandRotation),
//                    tint = Tasks.Configs.configs.themeColors.onPrimary
//                )
//                Icon(
//                    Icons.Default.ExitToApp,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .clickable {
//                            windowInteractions.expand.value = false
//                        }
//                        .padding(3.dp),
//                    tint = Tasks.Configs.configs.themeColors.onPrimary
//                )
//            }
        AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Tasks.Configs.configs.themeColors.primary.run {
                            Color(red, green, blue, .6f)
                        },
                        shape = RoundedCornerShape(5.dp)
                    )
            ) {
                BasicTextField(
                    value = searching,
                    onValueChange = {
                        searching = it
                        Tasks.filter("searching") { task ->
                            (task.title.contains(searching, true) ||
                                    task.deadline.contains(searching, true) ||
                                    task.description.contains(searching, true)) &&
                                    ((!task.done && !showDone) || showDone)
                        }
                    },
                    textStyle = TextStyle(
                        color = Tasks.Configs.configs.themeColors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 5.dp),
                    cursorBrush = SolidColor(Tasks.Configs.configs.themeColors.onPrimary)
                )
            }
        }

        AnimatedVisibility(visible = showAddMenu, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp, 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "➕ Tâche",
                    color = colors.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary.copy(alpha = 0.35f))
                        .clickable {
                            Tasks.update(Task())
                            showAddMenu = false
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Text(
                    "🤖 Ouvrir une session Claude",
                    color = colors.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary.copy(alpha = 0.35f))
                        .clickable { showClaudeLaunch = !showClaudeLaunch }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showAddMenu && showClaudeLaunch,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ClaudeLaunchPanel(colors) {
                showClaudeLaunch = false
                showAddMenu = false
            }
        }
    }
}