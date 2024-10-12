package com.elfefe.common.controller

import androidx.compose.ui.res.useResource
import com.elfefe.common.model.github.GithubLatestRelease
import com.elfefe.common.ui.view.Popup
import io.ktor.client.request.get
import io.ktor.util.InternalAPI
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.util.UUID
import kotlin.collections.firstOrNull

sealed class Updater(val message: String) {
    object Start : Updater("Launched TasksWidget update ! \uD83D\uDE80")
    object Download : Updater("Downloading the latest version of TasksWidget..")
    object Install : Updater("Installing the latest version of TasksWidget..")
    object Finished : Updater("TasksWidget will be closed to apply the update..")
    data class Error(val error: Throwable) : Updater("An error occurred while updating TasksWidget.. \uD83D\uDE1E")
}

val updaterScope = CoroutineScope(Dispatchers.IO)
var updateJob: Job? = null

fun installUpdater() {
    useResource("TasksWidget-Updater-1.0.jar") { updater ->
//        File(app, "TasksWidget-Updater-1.0.jar").writeBytes(updater.readAllBytes())
    }
}

@OptIn(InternalAPI::class)
fun update(release: GithubLatestRelease, onStatus: suspend CoroutineScope.(Updater) -> Unit) {
    updateJob = updaterScope.launch(Dispatchers.IO) {
        release.assets.firstOrNull()?.run {
            browserDownloadUrl?.let {
                try {
                    onStatus(Updater.Start)
                    val updateFile = File(tmpDir, "${UUID.randomUUID()}")

                    onStatus(Updater.Download)

                    client.get(it).content.copyAndClose(updateFile.writeChannel())
                    updateFile.renameTo(File(tmpDir, name ?: "TasksWidget-latest.msi"))

                    onStatus(Updater.Install)
                } catch (e: Exception) {
                    this@launch.log(e.stackTraceToString())
                    onStatus(
                        Updater.Error(
                            Exception(
                                "The latest version of TasksWidget could not be updated.. \uD83D\uDE1E \n" +
                                        "Please download and install it manually !"
                            )
                        )
                    )
                }
            }
        } ?: run {
            onStatus(
                Updater.Error(
                    Exception(
                        "The latest version of TasksWidget could not be updated manually.. \uD83D\uDE1E \n" +
                                "Please download and install it manually !"
                    )
                )
            )
        }
    }
}
