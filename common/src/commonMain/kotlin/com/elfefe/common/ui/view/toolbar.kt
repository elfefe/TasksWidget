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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import kotlinx.coroutines.CoroutineScope


@Composable
fun ColumnScope.Toolbar(scope: CoroutineScope, windowInteractions: WindowInteractions, toolbarInteractions: ToolbarInteractions) {

    var showConfigs by remember { mutableStateOf(false) }

    var showDone by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(true) }

    var expanded by remember { mutableStateOf(true) }
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f)

    var showSearch by remember { mutableStateOf(false) }

    var searching by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Tasks.Configs.configs.themeColors.primary,
                shape = RoundedCornerShape(5.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (expanded)
                Row {
                    Icon(
                        painterResource(if (showDescription) "baseline_notes_24.svg" else "short_text_24px.svg"),
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                showDescription = !showDescription
                                toolbarInteractions.showDescription(showDescription)
                            }
                            .padding(3.dp),
                        tint = Tasks.Configs.configs.themeColors.onPrimary
                    )
                    Icon(
                        painterResource(if (showDone)"check_circle_24px.svg" else "unpublished_24px.svg"),
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                showDone = !showDone
                                Tasks.filter { if (showDone) true else !it.done }
                            }
                            .padding(3.dp),
                        tint = Tasks.Configs.configs.themeColors.onPrimary.run {
                            if (showDone) copy(alpha = .6f) else this
                        }
                    )
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                Tasks.update(Task())
                            }
                            .padding(3.dp),
                        tint = Tasks.Configs.configs.themeColors.onPrimary
                    )
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
                                    "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"
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
                }

            Row(horizontalArrangement = Arrangement.End) {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            expanded = !expanded
                            windowInteractions.expand.value = expanded
                        }
                        .padding(3.dp)
                        .rotate(expandRotation),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            windowInteractions.visibility.value = false
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(3.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    windowInteractions.moveWindow.value = WindowMovement(true, it.x)
                                }
                            ) { change, dragAmount ->
                                windowInteractions.moveWindow.value = WindowMovement(false, 0f)
                            }
                        },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            }
        }
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
                        Tasks.filter { task ->
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
    }
}