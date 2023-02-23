package fr.exem.common.pager.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.AwtWindow
import com.elfefe.common.pager.page.PageImpl
import fr.exem.common.quality.s4.GeTe007
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.File


class Excel : PageImpl() {

    val geTe007 = GeTe007()

    enum class FileBrowserStates {
        FROM,
        TO,
        HIDE
    }

    class LogState(val text: String, val status: Status) {
        enum class Status {
            INFO,
            WARNING,
            ERROR
        }
    }

    @Composable
    override fun Show() {

        val scope = rememberCoroutineScope { Dispatchers.Default }

        var showFileBrowser by remember { mutableStateOf(FileBrowserStates.HIDE) }

        var fromPath by remember { mutableStateOf("") }
        var toPath by remember { mutableStateOf("") }

        var excelProgress by remember { mutableStateOf(listOf<LogState>()) }

        val progressState = rememberLazyListState()

        Column(
            modifier =
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedTextField(
                value = fromPath,
                onValueChange = {
                    fromPath = it
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f),
                leadingIcon = {
                    Text("From")
                },
                trailingIcon = {
                    IconButton(onClick = {
                        showFileBrowser = FileBrowserStates.FROM
                    }) {
                        Icon(Icons.Default.Search, "Search from", Modifier.size(24.dp))
                    }
                },
                singleLine = true
            )

            OutlinedTextField(
                value = toPath,
                onValueChange = {
                    toPath = it
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f),
                leadingIcon = {
                    Text("To")
                },
                trailingIcon = {
                    IconButton(onClick = {
                        showFileBrowser = FileBrowserStates.TO
                    }) {
                        Icon(Icons.Default.Search, "Search to", Modifier.size(24.dp))
                    }
                },
                singleLine = true
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.4f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = progressState
                    ) {
                        items(excelProgress) { log ->
                            Text(
                                text = log.text,
                                color = if (log.status == LogState.Status.ERROR) Color.Red else Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    geTe007.generateUsers(fromPath, toPath) {
                        excelProgress = excelProgress.toMutableList().apply { add(it) }
                        scope.launch { progressState.animateScrollToItem(excelProgress.size) }
                    }
                }
            ) {
                Text("Generate")
            }
        }
        if (showFileBrowser != FileBrowserStates.HIDE)
            FileBrowse(
                onFileSelected = {
                    when (showFileBrowser) {
                        FileBrowserStates.FROM -> fromPath = it
                        FileBrowserStates.TO -> toPath = it
                        else -> {}
                    }
                },
                onStateChange = {
                    showFileBrowser = FileBrowserStates.HIDE
                }, showFileBrowser == FileBrowserStates.TO
            )
    }

    @Composable
    private fun FileBrowse(
        onFileSelected: (String) -> Unit,
        onStateChange: (Boolean) -> Unit,
        loadFile: Boolean = true
    ) {
        AwtWindow(
            visible = true,
            create = {
                object : FileDialog(ComposeWindow(), "Choose a file", if (loadFile) LOAD else SAVE) {
                    override fun setVisible(value: Boolean) {
                        super.setVisible(value)
                        if (value) {
                            onFileSelected("$directory$file")
                        }
                    }
                }
            },
            dispose = {
                onStateChange(false)
            },
            update = {}
        )
    }
}