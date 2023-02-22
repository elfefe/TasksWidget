package com.elfefe.common.pager

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.elfefe.common.pager.page.PageImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface PageInteractions {
    fun onFinishedScroll(index: Int)
}

@Composable
fun Pager(
    modifier: Modifier =
        Modifier
            .fillMaxSize(),
    pages: List<PageImpl>,
    sensitivity: Float = 3f,
    onPageChanged: CoroutineScope.(Int) -> Unit = {}
): LazyListState {
    var pageIndex = 0

    var pagerWidth by remember { mutableStateOf(WindowState().size.width) }

    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = pageIndex)
    val coroutineScope = rememberCoroutineScope()

    LazyRow(
        modifier = modifier.then(
            Modifier
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > sensitivity && pageIndex > 0) pageIndex -= 1
                        else if (dragAmount < -sensitivity && pageIndex < pages.size - 1) pageIndex += 1
                        coroutineScope.launch {
                            onPageChanged(pageIndex)
                            scrollState.animateScrollToItem(pageIndex)
                        }
                    }
                }
                .onGloballyPositioned {
                    pagerWidth = it.size.width.dp
                }
        ),
        state = scrollState,
        userScrollEnabled = false
    ) {
        items(pages) { page ->
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(pagerWidth),
                contentAlignment = Alignment.Center
            ) {
                page.Show()
            }
        }
    }

    return scrollState
}