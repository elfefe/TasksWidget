package com.elfefe.common.controller

import androidx.compose.ui.res.useResource
import com.elfefe.common.model.github.GithubLatestRelease
import com.elfefe.common.ui.view.Popup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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

/** Découpe "v1.4.4" / "1.4.4-rc1" en [1, 4, 4] pour comparer proprement. */
fun versionParts(version: String): List<Int> =
    version.trim().removePrefix("v").removePrefix("V")
        .split('.', '-', '_', '+')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

/**
 * Vrai si la release distante est strictement plus récente que la version
 * embarquée. Le tag GitHub porte un "v" que la version locale n'a pas : sans
 * cette normalisation, "v1.4.4" et "1.4.4" étaient jugés différents et l'appli
 * se croyait perpétuellement périmée.
 */
fun isRemoteNewer(remoteTag: String?, localVersion: String?): Boolean {
    if (remoteTag == null || localVersion == null) return false
    val remote = versionParts(remoteTag)
    val local = versionParts(localVersion)
    if (remote.isEmpty()) return false
    for (i in 0 until maxOf(remote.size, local.size)) {
        val r = remote.getOrElse(i) { 0 }
        val l = local.getOrElse(i) { 0 }
        if (r != l) return r > l
    }
    return false
}

/**
 * Lance l'installation de façon détachée : un petit script attend que l'appli
 * se ferme, applique le MSI en silencieux, puis relance TasksWidget. Il survit
 * à la fermeture de l'appli (nécessaire pour que msiexec puisse remplacer les
 * fichiers verrouillés par le processus en cours).
 */
fun launchInstaller(installer: File) {
    val script = File(tmpDir, "update-${UUID.randomUUID()}.cmd")
    val exe = appFile.absolutePath
    script.writeText(
        buildString {
            append("@echo off\r\n")
            append("timeout /t 2 /nobreak >nul\r\n")
            append("msiexec /i \"${installer.absolutePath}\" /qb\r\n")
            append("start \"\" \"$exe\"\r\n")
        }
    )
    ProcessBuilder("cmd", "/c", "start", "", "/min", script.absolutePath)
        .directory(tmpDir)
        .start()
}

fun update(release: GithubLatestRelease, onStatus: suspend CoroutineScope.(Updater) -> Unit) {
    updateJob = updaterScope.launch(Dispatchers.IO) {
        release.assets.firstOrNull()?.run {
            browserDownloadUrl?.let {
                try {
                    onStatus(Updater.Start)
                    onStatus(Updater.Download)

                    // Téléchargement via java.net.http (module bundlé par
                    // jpackage) et non ktor CIO : dans le runtime réduit de
                    // l'installeur, CIO échoue alors que java.net.http fonctionne
                    // (c'est déjà lui qui récupère la release pour le bandeau).
                    val installer = File(tmpDir, name ?: "TasksWidget-latest.msi")
                    if (installer.exists()) installer.delete()
                    val http = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(30))
                        .build()
                    val request = HttpRequest.newBuilder(URI.create(it)).GET().build()
                    val response = http.send(request, HttpResponse.BodyHandlers.ofFile(installer.toPath()))
                    if (response.statusCode() !in 200..299)
                        throw IOException("Téléchargement impossible (HTTP ${response.statusCode()})")

                    // On démarre l'installeur détaché AVANT de signaler Install :
                    // le statut Install déclenche la fermeture de l'appli côté
                    // bandeau, et le script doit déjà tourner pour lui survivre.
                    launchInstaller(installer)
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
