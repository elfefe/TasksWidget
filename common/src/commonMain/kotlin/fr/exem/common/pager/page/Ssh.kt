package fr.exem.common.pager.page

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.elfefe.common.pager.page.PageImpl
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.event.KeyEvent
import java.io.ByteArrayOutputStream
import java.io.PrintStream


class Ssh : PageImpl() {
    private val session = JSch().getSession("elfefe", "localhost").apply {
        setPassword("elfefe")
        setConfig("StrictHostKeyChecking", "no")
        try {
            connect(CONNECTION_TIMEOUT)
        } catch (e: JSchException) {
            println(e.localizedMessage ?: "Jsch exception with non error message")
        }
    }

    private val communicationStream = ByteArrayOutputStream()

    private val scope = CoroutineScope(Dispatchers.Default)

    private fun executeSsh(command: String, onOutput: (String) -> Unit) {
        val channel = session.openChannel("shell").apply {
            outputStream = communicationStream
        }
        scope.launch(Dispatchers.IO) {
            val printStream = PrintStream(channel.outputStream, true)

            if (!session.isConnected)
                session.connect(CONNECTION_TIMEOUT)
            if (!channel.isConnected)
                channel.connect(CONNECTION_TIMEOUT)

            printStream.println(command)

            do {
                delay(20)

                onOutput(communicationStream.toByteArray().decodeToString())
                communicationStream.reset()
            } while (channel.isConnected)
        }
    }

    @Composable
    override fun Show() {
        var logs by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }

        Column(
            modifier =
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = {
                        command = it.removeSuffix("\n")
                    },
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth(0.7f)
                        .onKeyEvent {
                            if (it.key == Key(KeyEvent.VK_ENTER)) {
                                executeSsh(command) { text ->
                                    logs += text
                                }
                            }
                            true
                        },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        backgroundColor = Color.White,
                        cursorColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    )
                )
                IconButton(
                    onClick = {
                        executeSsh(command) {
                            logs += it
                        }
                    },
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                ) {
                    Icon(Icons.Default.Send, "", Modifier.size(24.dp))
                }
                IconButton(
                    onClick = {
                        executeSsh(command) {
                            logs = ""
                        }
                    },
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                ) {
                    Icon(Icons.Default.Delete, "", Modifier.size(24.dp))
                }
            }
            OutlinedTextField(
                value = logs,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxSize(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    backgroundColor = Color.Black,
                    cursorColor = Color.White,
                    unfocusedBorderColor = Color.White
                )
            )
        }
    }

    companion object {
        private const val CONNECTION_TIMEOUT = 1 * 60 * 1000
    }
}