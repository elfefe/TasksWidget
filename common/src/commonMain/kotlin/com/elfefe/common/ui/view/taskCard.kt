package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.dp
import com.elfefe.common.controller.Tasks
import com.elfefe.common.controller.deadlineDate
import com.elfefe.common.controller.scaledSp
import com.elfefe.common.model.Task
import java.io.File

@Composable
fun TaskCard(modifier: Modifier, task: Task, showDescription: Boolean) {
    var deadline by remember { mutableStateOf(task.deadline) }
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var done by remember { mutableStateOf(task.done) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .then(modifier),
        backgroundColor = Tasks.Configs.configs.themeColors.background,
        elevation = 5.dp
    ) {
        val deadlineDate = deadlineDate(task.deadline)
        Column(
            Modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
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

                        Tasks.update(task.apply { this.deadline = deadline })
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
                            offsetMapping = object: OffsetMapping {
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

                        Tasks.update(task.apply { this.title = it })
                    },
                    modifier = Modifier
                        .weight(1f),
                    textStyle = TextStyle(color = Tasks.Configs.configs.themeColors.onBackground, fontWeight = FontWeight.SemiBold),
                    singleLine = true
                )

                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(16.dp)
                        .clickable {

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
                            Tasks.update(task.apply { this.done = done })
                            Tasks.refresh()
                        },
                    tint = if (done) Color.Green else Color.Red
                )
            }
            AnimatedVisibility(
                visible = showDescription,
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
                        value = description,
                        onValueChange = {
                            description = it
                                .run {
                                    split("\n").map { line -> line.replace(regex = "^ - ".toRegex(), replacement = "• ") }.joinToString("\n")
                                }
                            Tasks.update(task.apply { this.description = it })
                        },
                        modifier = Modifier
                            .fillMaxSize(),
                        textStyle = TextStyle(color = Tasks.Configs.configs.themeColors.onBackground),
                        visualTransformation = {
                            TransformedText(
                                text = it,
                                offsetMapping = object: OffsetMapping {
                                    override fun originalToTransformed(offset: Int): Int {
                                        return offset
                                    }

                                    override fun transformedToOriginal(offset: Int): Int {
                                        return offset
                                    }
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}