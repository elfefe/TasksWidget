package fr.exem.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.exem.common.drawer.Drawer
import com.elfefe.common.pager.Pager
import fr.exem.common.pager.page.Ssh
import fr.exem.common.pager.page.Excel
import com.elfefe.common.pager.page.PageImpl
import fr.exem.common.theme.SMQToolboxTheme
import kotlinx.coroutines.launch

@Composable
fun App() {

    val pagesIndexed = mapOf(
        "GE-TE-007" to listOf(Excel()),
        "GE-TE-008" to listOf(Ssh()),
    )


    var currentIndex by remember { mutableStateOf(pagesIndexed.keys.iterator().next()) }

    var pageIndex by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    SMQToolboxTheme {
        Drawer(
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .widthIn(min = 256.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.3f)
                            .defaultMinSize(256.dp, 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("Login")
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .defaultMinSize(256.dp, 0.dp)
                    ) {
                        items(pagesIndexed.keys.toList()) { index ->
                            Row(
                                modifier = Modifier
                                    .fillParentMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = index,
                                    modifier = Modifier
                                        .clickable(
                                            role = Role.Tab
                                        ) {
                                            currentIndex = index
                                        }
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            },
            pageContent = { modifier ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(modifier)
                ) {
                    pagesIndexed.forEach { (index, pages) ->
                        if (currentIndex == index) {
                            Tabs(
                                pages = pages,
                                pageIndex = pageIndex,
                                onPageIndexChange = { pageIndex = it }
                            )
                            Pager(pages = pages) { pageIndex = it }
                                .apply { scope.launch { animateScrollToItem(pageIndex) } }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun Tabs(pages: List<PageImpl>, pageIndex: Int, onPageIndexChange: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = pageIndex,
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
    ) {
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

