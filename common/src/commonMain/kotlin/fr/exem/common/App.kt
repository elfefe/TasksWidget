package com.elfefe.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.elfefe.common.drawer.Drawer
import com.elfefe.common.pager.Pager
import com.elfefe.common.pager.page.Ssh
import com.elfefe.common.pager.page.News
import com.elfefe.common.pager.page.PageImpl
import kotlinx.coroutines.launch

@Composable
fun App() {

    val pages = listOf(Ssh(), News())

    var pageIndex by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    MaterialTheme {
        Drawer(
            drawerContent = {

            },
            pageContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Tabs(
                        pages = pages,
                        pageIndex = pageIndex,
                        onPageIndexChange = { pageIndex = it }
                    )
                    Pager(pages = pages) { pageIndex = it }
                        .apply { scope.launch { animateScrollToItem(pageIndex) } }
                }
            }
        )
    }
}

@Composable
fun Tabs(pages: List<PageImpl>, pageIndex: Int, onPageIndexChange: (Int) -> Unit) {
    TabRow(pageIndex) {
        pages.forEach { page ->
            Tab(
                selected = pages.indexOf(page) == pageIndex,
                onClick = {
                    onPageIndexChange(pages.indexOf(page))
                },
                text = {
                    Text(page.javaClass.simpleName)
                }
            )
        }
    }
}

