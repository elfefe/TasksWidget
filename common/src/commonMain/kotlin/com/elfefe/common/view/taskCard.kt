package com.elfefe.common.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elfefe.common.controller.Tasks
import com.elfefe.common.controller.deadlineDate
import com.elfefe.common.model.Task
import kotlinx.coroutines.CoroutineScope

@Composable
fun TaskCard(modifier: Modifier, task: Task, showDescription: Boolean) {
    var deadline by remember { mutableStateOf(task.deadline) }
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .then(modifier),
        backgroundColor = Color.White,
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
                        if (it.isBlank()) {
                            deadline = "0"
                            return@BasicTextField
                        }

                        if (it.length <= 4 && it.last().digitToIntOrNull() != null)
                            deadline = it.replace("/", "")

                        if (it.length == 4 && !it.contains("/"))
                            deadline = it.substring(0, 2) + "/" + it.substring(2, 4)

                        Tasks.update(task.apply { this.deadline = deadline })
                    },
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(10.dp, 0.dp),
                    textStyle = TextStyle(
                        color =
                        if (task.done || deadlineDate == 1) Color.Black
                        else if (deadlineDate == -1) Color(0xFFFFB900)
                        else Color.Red,
                        fontSize = 10.scaledSp(),
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true
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
                    textStyle = TextStyle(fontWeight = FontWeight.SemiBold),
                    singleLine = true
                )

                Icon(
                    if (task.done) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(16.dp)
                        .clickable {
                            Tasks.update(task.apply { done = !done })
                        },
                    tint = if (task.done) Color.Green else Color.Red
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
                            Tasks.update(task.apply { this.description = it })
                        },
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}