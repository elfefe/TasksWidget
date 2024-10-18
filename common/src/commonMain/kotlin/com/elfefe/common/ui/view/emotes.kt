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

@Composable
fun Emotes(windowInteractions: WindowInteractions) {
    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
    ) {
        val clipboardManager: ClipboardManager = LocalClipboardManager.current

        val categoryScrollState = rememberLazyListState()

        val emojis by remember { mutableStateOf(EmojiApi.emojis) }

        LazyColumn(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize(),
            state = categoryScrollState,
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(emojis) { index, category ->
                Column(
                    modifier = Modifier
                ) {
                    var isSelected by remember { mutableStateOf(index == 0) }

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
