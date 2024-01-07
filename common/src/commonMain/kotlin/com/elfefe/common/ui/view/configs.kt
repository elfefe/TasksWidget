package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
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
import com.elfefe.common.model.TaskFieldOrder
import com.elfefe.common.ui.theme.primary
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class ConfigNavDestination(val text: String) {
    EMOTES("Emotes"),
    CARDS("Cards"),
    THEMES("Theme"),
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
    AnimatedNavigation(destination == ConfigNavDestination.THEMES) { Theme() }
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
    val sortOrders =
        remember { mutableStateOf(Tasks.Configs.configs.taskFieldsOrder.sortedByDescending { it.priority }) }

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
                        .width(256.dp)
                        .height(if (state.draggingItemIndex == index) 48.dp else 42.dp),
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
                Tasks.Configs.configs.taskFieldsOrder.filter { it.name == taskField.name }.onEach {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Theme() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            stickyHeader {
                Text(
                    text = "Primary",
                    fontWeight = FontWeight.Thin,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
            }
            item {
                ThemeColor {

                }
            }
            stickyHeader {
                Text(
                    text = "On primary",
                    fontWeight = FontWeight.Thin,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
            }
            item {
                ThemeColor {

                }
            }
            stickyHeader {
                Text(
                    text = "Secondary",
                    fontWeight = FontWeight.Thin,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
            }
            item {
                ThemeColor {

                }
            }
            stickyHeader {
                Text(
                    text = "On secondary",
                    fontWeight = FontWeight.Thin,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
            item {
                ThemeColor {

                }
            }
            stickyHeader {
                Text(
                    text = "Background",
                    fontWeight = FontWeight.Thin,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
            item {
                ThemeColor {

                }
            }
            stickyHeader {
                Text(
                    text = "On background",
                    fontWeight = FontWeight.Thin,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
            item {
                ThemeColor {

                }
            }
        }
    }
}

@Composable
fun ThemeColor(onColorChange: (Color) -> Unit) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .width(256.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var currentColor by remember { mutableStateOf(Color.Transparent) }
        var saturationColor by remember { mutableStateOf(Color.Transparent) }

        Column(
            modifier = Modifier
                .width(180.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            var colorCursorPosition by remember { mutableStateOf(0f) }
            var darknessCursorPosition by remember { mutableStateOf(90f) }
            var alphaCursorPosition by remember { mutableStateOf(1f) }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            colorCursorPosition = change.position.x
                        }
                    }
            ) {
                val colorGradient =
                    listOf(
                        Color(1f,0f,0f),
                        Color(1f,1f,0f),
                        Color(0f,1f,0f),
                        Color(0f,1f,1f),
                        Color(0f,0f,1f),
                        Color(1f,0f,1f),
                    )
                val colorAlphaGradient =
                    listOf(
                        Color(1f,0f,0f, alphaCursorPosition),
                        Color(1f,1f,0f, alphaCursorPosition),
                        Color(0f,1f,0f, alphaCursorPosition),
                        Color(0f,1f,1f, alphaCursorPosition),
                        Color(0f,0f,1f, alphaCursorPosition),
                        Color(1f,0f,1f, alphaCursorPosition),
                    )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorGradient,
                        tileMode = TileMode.Clamp
                    ),
                    size = size
                )
                drawRect(
                    color = Color.DarkGray,
                    topLeft = Offset(max(0f, min(size.width, colorCursorPosition)), 0f),
                    size = Size(2f, size.height)
                )

                val colorIndexNormalized = (colorCursorPosition / size.width).let { if (it.isNaN()) 0f else it }
                val firstColorIndex = max(0, min(colorAlphaGradient.size - 2 ,
                    floor((colorIndexNormalized) * (colorAlphaGradient.size - 1)).toInt()
                ))

                val fractionStep = 1f / (colorAlphaGradient.size - 1)
                val minFraction = ((firstColorIndex + 1) / (colorAlphaGradient.size - 1f)) - fractionStep
                val colorFraction = max(0f, min(1f, (colorIndexNormalized - minFraction) * (colorAlphaGradient.size - 1f)))

                currentColor = if (colorAlphaGradient.lastIndex == firstColorIndex) colorAlphaGradient[firstColorIndex]
                else lerp(colorAlphaGradient[firstColorIndex], colorAlphaGradient[firstColorIndex + 1], colorFraction)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            darknessCursorPosition = change.position.x
                        }
                    }
            ) {
                val colorGradient = listOf(Color.Black, currentColor, Color.White)
                val colorAlphaGradient = listOf(
                    Color(0f, 0f, 0f, alphaCursorPosition),
                    currentColor,
                    Color(1f, 1f, 1f, alphaCursorPosition)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorGradient,
                        tileMode = TileMode.Clamp
                    ),
                    size = size
                )
                drawRect(
                    color = Color.primary,
                    topLeft = Offset(max(0f, min(size.width, darknessCursorPosition)), 0f),
                    size = Size(2f, size.height)
                )

                val indexNormalized = (darknessCursorPosition / size.width).let {
                    if (it.isNaN()) 0f else min(1f, max(0f, it)) }
                val colorIndex = min(floor(indexNormalized * (colorAlphaGradient.size - 1)).toInt(), colorAlphaGradient.size - 2)

                val fractionStep = 1f / (colorAlphaGradient.size - 1)
                val minFraction = ((colorIndex + 1) / (colorAlphaGradient.size - 1f)) - fractionStep
                val colorLerpFraction = max(0f, min(1f, (indexNormalized - minFraction) * (colorAlphaGradient.size - 1f)))

                saturationColor = lerp(colorAlphaGradient[colorIndex], colorAlphaGradient[colorIndex + 1], colorLerpFraction)
            }

            Slider(
                value = alphaCursorPosition,
                onValueChange = { alphaCursorPosition = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .size(64.dp)
        ) {
            onColorChange(saturationColor)
            drawRoundRect(
                color = saturationColor,
                cornerRadius = CornerRadius(8f, 8f),
                size = size
            )
        }
    }
}