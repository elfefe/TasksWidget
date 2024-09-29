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
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.*
import com.elfefe.common.model.TaskFieldOrder
import com.elfefe.common.ui.theme.primary
import grayScale
import hexToColor
import maxSaturation
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import toHexString
import java.io.File
import java.nio.file.Files
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class ConfigNavDestination(val text: String) {
    EMOTES(Traductions().emotes),
//    CARDS(Traductions().cards),
    THEMES(Traductions().theme),
    GENERAL(Traductions().general),
}

@Composable
fun Configs(windowInteractions: WindowInteractions) {
    var currentDestination: ConfigNavDestination by remember { mutableStateOf(ConfigNavDestination.EMOTES) }

    Row {
        NavigationBar {
            currentDestination = it
        }
        Navigator(currentDestination, windowInteractions)
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
fun Navigator(destination: ConfigNavDestination, windowInteractions: WindowInteractions) {
    AnimatedNavigation(destination == ConfigNavDestination.EMOTES) { Emotes(windowInteractions) }
//    AnimatedNavigation(destination == ConfigNavDestination.CARDS) { Cards(windowInteractions) }
    AnimatedNavigation(destination == ConfigNavDestination.THEMES) { Theme(windowInteractions) }
    AnimatedNavigation(destination == ConfigNavDestination.GENERAL) { General(windowInteractions) }
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
fun Emotes(windowInteractions: WindowInteractions) {
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
                    }
                    ) {
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
                                                        windowInteractions.popup.value =
                                                            Popup.show("Copied ${emoji.character}")
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
fun Cards(windowInteractions: WindowInteractions) {
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

                Tasks.refresh()
                Tasks.Configs.update()
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

@Composable
fun Theme(windowInteractions: WindowInteractions) {
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
            themePartConfig(Traductions().toolbarBackground, Tasks.Configs.configs.themeColors.primary) {
                Tasks.Configs.configs.updateThemeColors(primary = it)
                Tasks.Configs.update()
            }
            themePartConfig(Traductions().toolbarIcons, Tasks.Configs.configs.themeColors.onPrimary) {
                Tasks.Configs.configs.updateThemeColors(onPrimary = it)
                Tasks.Configs.update()
            }
            /*themePartConfig("Secondary", Tasks.Configs.configs.themeColors.secondary) {
                Tasks.Configs.configs.updateThemeColors(secondary = it)
                Tasks.Configs.update()
            }
            themePartConfig("On secondary", Tasks.Configs.configs.themeColors.onSecondary) {
                Tasks.Configs.configs.updateThemeColors(onSecondary = it)
                Tasks.Configs.update()
            }*/
            themePartConfig(Traductions().tasksBackground, Tasks.Configs.configs.themeColors.background) {
                Tasks.Configs.configs.updateThemeColors(background = it)
                Tasks.Configs.update()
            }
            themePartConfig(Traductions().tasksContent, Tasks.Configs.configs.themeColors.onBackground) {
                Tasks.Configs.configs.updateThemeColors(onBackground = it)
            }
        }
    }
}

@Composable
fun General(windowInteractions: WindowInteractions) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        var startOnBoot by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .height(256.dp)
                .fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = startOnBoot, onCheckedChange = {
                        startOnBoot = it
                        val startupAppPath = "C:\\$userPath\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\" +
                                "Programs\\Startup\\TasksWidget.exe"
                        if (it) {
                            try {
                                if (appDir.exists())
                                    if (!File(startupAppPath).exists())
                                        Files.createSymbolicLink(
                                            File(startupAppPath).toPath(),
                                            appDir.toPath(),
                                        )
                            } catch (e: Exception) {
                                windowInteractions.popup.value = Popup.show(e.message ?: "Error while creating link")
                            }
                        } else {
                            try {
                                if (File(startupAppPath).exists())
                                    File(startupAppPath).delete()
                            } catch (e: Exception) {
                                windowInteractions.popup.value = Popup.show(e.message ?: "Error while deleting link")
                            }
                        }
                    })

                    Spacer(Modifier.width(16.dp))

                    Text(
                        text = Traductions().startupLabel,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.themePartConfig(label: String, defaultColor: Color, onColorChange: (Color) -> Unit) {
    stickyHeader {
        Text(
            text = label,
            fontWeight = FontWeight.Thin,
            fontSize = 18.sp,
            color = Color.DarkGray
        )
    }
    item {
        ThemeColor(defaultColor, onColorChange)
    }
}

@Composable
fun ThemeColor(default: Color, onColorChange: (Color) -> Unit) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .width(256.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var currentColor by remember { mutableStateOf(default) }

        var colorCursorPosition by remember { mutableStateOf(0f) }
        var darknessCursorPosition by remember { mutableStateOf((default.red + default.blue + default.green) / 3 * 180) }
        var alphaCursorPosition by remember { mutableStateOf(default.alpha) }
        var saturated by remember { mutableStateOf(default.red != default.blue && default.blue != default.green) }

        Column(
            modifier = Modifier
                .width(180.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val colorGradient = listOf(
                Color(1f, 0f, 0f),
                Color(1f, 1f, 0f),
                Color(0f, 1f, 0f),
                Color(0f, 1f, 1f),
                Color(0f, 0f, 1f),
                Color(1f, 0f, 1f),
                Color(1f, 0f, 0f),
            )

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
                val colorAlphaGradient =
                    listOf(
                        Color(1f, 0f, 0f, alphaCursorPosition),
                        Color(1f, 1f, 0f, alphaCursorPosition),
                        Color(0f, 1f, 0f, alphaCursorPosition),
                        Color(0f, 1f, 1f, alphaCursorPosition),
                        Color(0f, 0f, 1f, alphaCursorPosition),
                        Color(1f, 0f, 1f, alphaCursorPosition),
                        Color(1f, 0f, 0f, alphaCursorPosition),
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
                val firstColorIndex = max(
                    0, min(
                        colorAlphaGradient.size - 2,
                        floor((colorIndexNormalized) * (colorAlphaGradient.size - 1)).toInt()
                    )
                )

                val fractionStep = 1f / (colorAlphaGradient.size - 1)
                val minFraction = ((firstColorIndex + 1) / (colorAlphaGradient.size - 1f)) - fractionStep
                val colorFraction =
                    max(0f, min(1f, (colorIndexNormalized - minFraction) * (colorAlphaGradient.size - 1f)))

                if (saturated) currentColor =
                    if (colorAlphaGradient.lastIndex == firstColorIndex) colorAlphaGradient[firstColorIndex]
                    else lerp(
                        colorAlphaGradient[firstColorIndex],
                        colorAlphaGradient[firstColorIndex + 1],
                        colorFraction
                    )
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
                val colorGradient = listOf(Color.Black, if (saturated) currentColor else Color.Gray, Color.White)
                val colorAlphaGradient = listOf(
                    Color(0f, 0f, 0f, alphaCursorPosition),
                    if (saturated) currentColor.maxSaturation() else Color.Gray,
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
                    color = currentColor.grayScale().run { Color(
                        (1 - red).absoluteValue.coerceIn(0f, 1f),
                        (1 - green).absoluteValue.coerceIn(0f, 1f),
                        (1 - blue).absoluteValue.coerceIn(0f, 1f),
                        1f
                    ) },
                    topLeft = Offset(max(0f, min(size.width, darknessCursorPosition)), 0f),
                    size = Size(2f, size.height)
                )

                val indexNormalized = (darknessCursorPosition / size.width).let {
                    if (it.isNaN()) 0f else min(1f, max(0f, it))
                }
                val colorIndex =
                    min(floor(indexNormalized * (colorAlphaGradient.size - 1)).toInt(), colorAlphaGradient.size - 2)

                val fractionStep = 1f / (colorAlphaGradient.size - 1)
                val minFraction = ((colorIndex + 1) / (colorAlphaGradient.size - 1f)) - fractionStep
                val colorLerpFraction =
                    max(0f, min(1f, (indexNormalized - minFraction) * (colorAlphaGradient.size - 1f)))

                currentColor =
                    lerp(colorAlphaGradient[colorIndex], colorAlphaGradient[colorIndex + 1], colorLerpFraction)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Slider(
                    value = alphaCursorPosition,
                    onValueChange = { alphaCursorPosition = it },
                    modifier = Modifier
                        .fillMaxWidth(.8f)
                        .fillMaxHeight()
                )

                Spacer(Modifier.width(4.dp))

                Checkbox(saturated, { saturated = !saturated })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Canvas(
                modifier = Modifier
                    .size(64.dp)
            ) {
                onColorChange(currentColor)
                drawRoundRect(
                    color = currentColor,
                    cornerRadius = CornerRadius(8f, 8f),
                    size = size
                )
            }

            Spacer(Modifier.height(8.dp))

            BasicTextField(
                value = currentColor.toHexString(),
                onValueChange = {
                    println(it)
                    var colorText = it
                    if (colorText.startsWith("#"))
                        colorText = colorText.substring(1)
                    if (colorText.length != 8) return@BasicTextField

                    try {
                        currentColor = colorText.hexToColor()
                        onColorChange(currentColor)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                readOnly = false,
                modifier = Modifier
                    .padding(0.dp)
                    .width(64.dp)
                    .align(Alignment.CenterHorizontally),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 12.sp
                ),
                singleLine = true,
                visualTransformation = {
                    TransformedText(
                        text = AnnotatedString("#${it.text}"),
                        offsetMapping = object :OffsetMapping {
                            override fun originalToTransformed(offset: Int): Int {
                                return offset + 1
                            }

                            override fun transformedToOriginal(offset: Int): Int {
                                return when {
                                    offset == 1 -> 0
                                    offset > it.text.length -> it.text.length
                                    else -> offset
                                }
                            }

                        }
                    )
                }
            )
        }
    }
}