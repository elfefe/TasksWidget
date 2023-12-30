package com.elfefe.common.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.elfefe.common.controller.EmojiApi
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import com.elfefe.common.model.TaskFieldOrder
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

enum class ConfigNavDestination(val text: String) {
    EMOTES("Emotes"),
    CARDS("Cards")
}

@Composable
fun Configs() {
    var currentDestination: ConfigNavDestination by remember { mutableStateOf(ConfigNavDestination.EMOTES) }

    Row {
        NavigationBar {
            currentDestination = it
        }
        Navigator(currentDestination)
    }
}

@Composable
fun AnimatedNavigation(visible: Boolean, page: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) { page() }
}

@Composable
fun Navigator(destination: ConfigNavDestination) {
    AnimatedNavigation(destination == ConfigNavDestination.EMOTES) { Emotes() }
    AnimatedNavigation(destination == ConfigNavDestination.CARDS) { Cards() }
}

@Composable
fun NavigationBar(onNavigate: (ConfigNavDestination) -> Unit) {
    Column(
        modifier = Modifier
            .background(color = Color(0xFF2C2F33))
            .padding(16.dp)
            .fillMaxHeight()
            .width(128.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(ConfigNavDestination.values()) { destination ->
                TextButton(onClick = {
                    onNavigate(destination)
                }) {
                    Text(
                        text = destination.text,
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun Emotes() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val clipboardManager: ClipboardManager = LocalClipboardManager.current

        val categoryScrollState = rememberLazyListState()

        val emojis by remember { mutableStateOf(EmojiApi.emojis) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = categoryScrollState,
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(emojis) { index, category ->
                Column(
                    modifier = Modifier
                ) {
                    var isSelected by remember { mutableStateOf(false) }

                    TextButton(onClick = {
                        isSelected = !isSelected
                    }) {
                        Text(
                            text = category.name,
                            modifier = Modifier
                                .padding(4.dp),
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }

                    AnimatedVisibility(
                        visible = isSelected,
                        modifier = Modifier
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            category.subCategories.forEach { subCategory ->
                                Row(
                                    modifier = Modifier
                                        .padding(2.dp)
                                ) {
                                    subCategory.emojis.forEach { emoji ->
                                        if (emoji.type == "default")
                                            Text(
                                                text = emoji.character,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(emoji.character))
                                                    }
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Cards() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        CardsOrder()
    }
}

@Composable
fun CardsOrder() {
    val sortOrders = remember { mutableStateOf(Tasks.Configs.configs.taskFieldOrders.sortedByDescending { it.priority }) }

    val state = rememberReorderableLazyListState(onMove = { from, to ->
        sortOrders.value = sortOrders.value.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    })

    LazyColumn(
        state = state.listState,
        modifier = Modifier
            .reorderable(state)
            .detectReorderAfterLongPress(state)
    ) {
        itemsIndexed(sortOrders.value) { index, taskField ->
            taskField.priority = when {
                taskField.priority > 0 -> index
                taskField.priority < 0 -> -sortOrders.value.size + index - 1
                else -> 0
            }
            ReorderableItem(state, key = taskField) {
                CardsOrderCondition(
                    modifier = Modifier
                        .padding(8.dp)
                        .width(128.dp)
                        .height(if (state.draggingItemIndex == index) 42.dp else 32.dp),
                    elevation = if (state.draggingItemIndex == index) 8.dp else 2.dp,
                    taskField = taskField
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CardsOrderCondition(modifier: Modifier, elevation: Dp, taskField: TaskFieldOrder) {
    Card(modifier = modifier, elevation = elevation) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var orderActive by remember { mutableStateOf(taskField.active) }
            var orderRotation by remember { mutableStateOf(if (taskField.priority > 0) 180f else 0f) }
            val orderRotationAnimation by animateFloatAsState(if (orderRotation == 0f) 180f else 0f)

            fun updateConfigs() {
                Tasks.Configs.configs.taskFieldOrders.filter { it.name == taskField.name }.onEach {
                    it.active = taskField.active
                    it.priority = taskField.priority
                }

                Tasks.Configs.update(Tasks.Configs.configs)
            }

            Checkbox(checked = orderActive, onCheckedChange = {
                orderActive = it
                taskField.active = it

                updateConfigs()
            })

            Text(
                text = taskField.name,
                fontWeight = FontWeight.Thin,
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            IconButton({
                orderRotation = if (orderRotation == 0f) 180f else 0f

                if (taskField.priority != 0)
                    taskField.priority *= -1

                updateConfigs()
            }) {
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint = Color.Black,
                    modifier = Modifier.rotate(orderRotationAnimation)
                )
            }
        }
    }
}