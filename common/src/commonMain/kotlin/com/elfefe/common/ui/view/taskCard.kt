package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.*
import com.elfefe.common.model.Task
import com.google.common.collect.EvictingQueue
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.text.substring

class TaskCardManager(val task: Task) {
    var selections by mutableStateOf(EvictingQueue.create<TextRange>(2))
    var description by mutableStateOf(TextFieldValue(task.description))
    var showEditor by mutableStateOf(false)
    var showDescription by mutableStateOf(false)
}


@Composable
fun TaskCard(modifier: Modifier, task: Task, windowInteractions: WindowInteractions, showDescription: Boolean) {
    val taskCardManager = TaskCardManager(task)
    taskCardManager.showDescription = showDescription

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .then(modifier),
        backgroundColor = Tasks.Configs.configs.themeColors.background,
        elevation = 5.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
            TopBar(taskCardManager)
            Editor(taskCardManager, windowInteractions)
            Content(taskCardManager)
        }
    }
}

@Composable
fun TopBar(manager: TaskCardManager) {
    var deadline by remember { mutableStateOf(manager.task.deadline) }
    var title by remember { mutableStateOf(manager.task.title) }
    var done by remember { mutableStateOf(manager.task.done) }
    val deadlineDate = deadlineDate(manager.task.deadline)

    Row(
        Modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BasicTextField(
            value = deadline,
            onValueChange = {
                var corrected = it

                if (it.any { char -> !char.isDigit() })
                    corrected = it.filter { char -> char.isDigit() }

                if (it.length < 4)
                    corrected += "0".repeat(4 - it.length)


                if (it.length > 4)
                    corrected = it.substring(0, 4)

                deadline = corrected

                Tasks.update(manager.task.apply { this.deadline = deadline })
            },
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(10.dp, 0.dp),
            textStyle = TextStyle(
                color =
                if (done || deadlineDate == 1) Tasks.Configs.configs.themeColors.onBackground
                else if (deadlineDate == -1) Color(0xFFFFB900)
                else Color.Red,
                fontSize = 10.scaledSp(),
                fontWeight = FontWeight.SemiBold
            ),
            singleLine = true,
            visualTransformation = {
                TransformedText(
                    text = AnnotatedString(it.text.run {
                        if (length < 2) return@run this
                        substring(0, 2) + "/" + substring(2)
                    }),
                    offsetMapping = object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
                            return when (offset) {
                                0 -> 0
                                1 -> 1
                                2 -> 3
                                3 -> 4
                                4 -> 5
                                else -> 5
                            }
                        }

                        override fun transformedToOriginal(offset: Int): Int {
                            return when (offset) {
                                0 -> 0
                                1 -> 1
                                2 -> 2
                                3 -> 2
                                4 -> 3
                                5 -> 4
                                else -> 4
                            }
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.width(3.dp))

        BasicTextField(
            value = title,
            onValueChange = {
                title = it

                Tasks.update(manager.task.apply { this.title = it })
            },
            modifier = Modifier
                .weight(1f),
            textStyle = TextStyle(
                color = Tasks.Configs.configs.themeColors.onBackground,
                fontWeight = FontWeight.SemiBold
            ),
            singleLine = true
        )

        Icon(
            painterResource(if (!manager.showEditor) "edit.svg" else "edit_off.svg"),
            contentDescription = "Edit",
            modifier = Modifier
                .clip(CircleShape)
                .size(16.dp)
                .clickable {
                    manager.showEditor = !manager.showEditor
                },
            tint = Tasks.Configs.configs.themeColors.onBackground
        )

        Spacer(Modifier.width(4.dp))

        Icon(
            if (done) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(16.dp)
                .clickable {
                    done = !done
                    Tasks.update(manager.task.apply { this.done = done })
                    Tasks.refresh()
                },
            tint = if (done) Color.Green else Color.Red
        )
    }
}

@Composable
fun Editor(manager: TaskCardManager, windowInteractions: WindowInteractions) {
    AnimatedVisibility(
        visible = manager.showEditor,
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Text Button
                item {
                    val level = 1
                    Text(
                        text = "H$level",
//                        fontSize = (24 - level * 2).sp,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                toggleHeader(manager, level)
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Bold Text Button
                item {
                    Text(
                        text = "B",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                wrapSelectionWith(manager, "**")
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Italic Text Button
                item {
                    Text(
                        text = "I",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                wrapSelectionWith(manager, "_")
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Strikethrough Button
                item {
                    Text(
                        text = "S",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                wrapSelectionWith(manager, "~~")
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Inline Code Button
                /*item {
                    Text(
                        text = "{ }",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                wrapSelectionWith(manager, "`")
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }*/

                // Bullet List Button
                item {
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                toggleList(manager, "bullet")
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Numbered List Button
                item {
                    Text(
                        text = "1.",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                toggleList(manager, "numbered")
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Code Block Button
                /*item {
                    Text(
                        text = "```",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                toggleCodeBlock(manager)
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }*/

                // Highlight Button
                item {
                    Text(
                        text = "HL",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Yellow)
                            .size(16.dp)
                            .clickable {
                                toggleHighlight(manager)
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // Emotes Button
                item {
                    Text(
                        text = "\uD83D\uDE42",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                windowInteractions.showEmotes.value = !(windowInteractions.showEmotes.value ?: true)
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun Content(manager: TaskCardManager) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = manager.showDescription,
        modifier = Modifier
            .fillMaxWidth(),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            Modifier
                .fillMaxSize()
        ) {
            BasicTextField(
                value = manager.description,
                onValueChange = {
                    manager.description = it
                    manager.selections.add(it.selection)
                    Tasks.update(manager.task.apply { this.description = it.text })
                },
                modifier = Modifier
                    .fillMaxSize(),
                textStyle = TextStyle(color = Tasks.Configs.configs.themeColors.onBackground),
                visualTransformation = MarkdownVisualTransformation(),
//                interactionSource = remember { MutableInteractionSource() }.apply {
//                    interactions.onEach {
//                        println("Interaction: ${it}")
//                    }.launchIn(scope)
//                },
            )
        }
    }
}