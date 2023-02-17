import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

@Composable
@Preview
fun Toolbox() {
    var sendMailsLog by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Send mails")

    val lazyPagerState = rememberLazyListState(0, 0)

    val scope = rememberCoroutineScope { Dispatchers.Default }

    val pages = listOf<@Composable (Array<Any>) -> Unit> { SendMails(it[0] as String, it[1] as CoroutineScope, it[2] as (String) -> Unit) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            TabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = {
                            Text(title)
                        },
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            scope.launch {
                                lazyPagerState.animateScrollToItem(index)
                            }
                        }
                    )
                }
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxSize(),
                state = lazyPagerState
            ) {
                items(pages) { it ->
                    it(arrayOf(sendMailsLog, scope, { text: String -> sendMailsLog = text}))
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        Toolbox()
    }
}

@Composable
fun SendMails(
    text: String,
    scope: CoroutineScope,
    output: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            output("...")
            scope.launch(Dispatchers.IO) {
                sendMails {
                    output(it)
                }
            }
        }) {
            Text("Relance des mails")
        }
        OutlinedTextField(
            value = text,
            onValueChange = {},
            modifier = Modifier
                .fillMaxSize(),
            readOnly = true
        )
    }
}

fun sendMails(onCommand: (String) -> Unit) {
    val sendMailsCommand = "/bin/bash /home/wavescanner/gestion_livrable/process_envoi_mail/traiteRapportComplet.sh"
    SSHClient().run {
        addHostKeyVerifier(PromiscuousVerifier())
        connect("10.252.111.15")
        authPassword("wavescanner", "FG27xXd9R696cKjj")
        startSession().run Session@ {
            onCommand(
                exec(sendMailsCommand)
                    .inputStream
                    .readBytes()
                    .decodeToString()
            )
            disconnect()
        }
    }

}
