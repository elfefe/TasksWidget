package com.elfefe.common.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.useResource
import com.elfefe.common.model.github.GithubLatestRelease
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Mise à jour **automatique**.
 *
 * Il n'y en avait aucune : la version distante n'était interrogée qu'une fois au
 * démarrage, le bandeau qui l'annonçait n'était visible que la pile déployée, et
 * il fallait cliquer dessus pour lancer quoi que ce soit. Une application posée
 * sur le bord de l'écran et laissée ouverte des jours durant ne voyait donc
 * jamais passer une version.
 *
 * Désormais : relevé au démarrage puis toutes les [CHECK_INTERVAL_MS], et
 * installation d'elle-même. L'installeur étant par-utilisateur, `msiexec`
 * s'exécute sans élévation ni la moindre invite.
 */
object AutoUpdater {

    /** Deux heures : assez pour ne rien manquer, assez peu pour rester discret. */
    const val CHECK_INTERVAL_MS = 2 * 60 * 60 * 1000L

    /** Nouvelle tentative après un échec réseau (coupure, GitHub indisponible). */
    const val RETRY_INTERVAL_MS = 15 * 60 * 1000L

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/elfefe/TasksWidget/releases/latest"

    sealed interface State {
        /** Rien à signaler. */
        object Idle : State

        /** Une version est disponible mais l'application est occupée. */
        data class Waiting(val version: String) : State
        data class Downloading(val version: String) : State
        data class Installing(val version: String) : State
        data class Failed(val reason: String) : State
    }

    var state by mutableStateOf<State>(State.Idle)
        private set

    /** Version embarquée dans le binaire, écrite au moment du packaging. */
    val currentVersion: String by lazy {
        runCatching { useResource("version") { it.readBytes().toString(Charsets.UTF_8) } }
            .getOrNull()?.trim().orEmpty()
    }

    /** Version déjà installée par nos soins : on ne la réapplique pas en boucle. */
    private var installedVersion: String? = null

    private val http: HttpClient by lazy {
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()
    }

    /** Dernière release publiée, ou `null` si GitHub est injoignable. */
    fun latestRelease(): GithubLatestRelease? = runCatching {
        val response = http.send(
            HttpRequest.newBuilder(URI.create(LATEST_RELEASE_URL)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() !in 200..299) null
        else Gson().fromJson(response.body(), GithubLatestRelease::class.java)
    }.onFailure { log("AutoUpdater: release distante illisible\n" + it.stackTraceToString()) }
        .getOrNull()

    /** Une version est prête, l'application attend un moment propice. */
    fun awaiting(version: String) {
        state = State.Waiting(version)
    }

    /** Cette release apporte-t-elle quelque chose de plus récent ? */
    fun isWorthInstalling(release: GithubLatestRelease?): Boolean {
        val tag = release?.tagName ?: return false
        if (tag == installedVersion) return false
        return isRemoteNewer(tag, currentVersion)
    }

    /**
     * Télécharge puis applique la mise à jour. Rend `true` si l'installeur a été
     * lancé — auquel cas l'application doit se fermer pour que `msiexec` puisse
     * remplacer les fichiers qu'elle tient ouverts ; le script la relance seul.
     */
    fun install(release: GithubLatestRelease): Boolean {
        val version = release.tagName ?: return false
        val asset = release.assets.firstOrNull()
        val url = asset?.browserDownloadUrl
        if (url == null) {
            state = State.Failed("Aucun installeur dans la release $version")
            log("AutoUpdater: release $version sans asset téléchargeable")
            return false
        }

        return runCatching {
            state = State.Downloading(version)
            val installer = File(tmpDir, asset.name ?: "TasksWidget-latest.msi")
            if (installer.exists()) installer.delete()

            val response = http.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofFile(installer.toPath())
            )
            if (response.statusCode() !in 200..299)
                throw IOException("Téléchargement impossible (HTTP ${response.statusCode()})")

            state = State.Installing(version)
            installedVersion = version
            launchSilentInstaller(installer)
            true
        }.onFailure {
            state = State.Failed("Mise à jour $version impossible")
            log("AutoUpdater: mise à jour $version impossible\n" + it.stackTraceToString())
        }.getOrDefault(false)
    }

    /**
     * Applique le MSI sans un bruit, puis relance l'application.
     *
     * Le script est détaché : il doit survivre à la fermeture de l'application,
     * seule façon pour `msiexec` de remplacer des fichiers verrouillés par le
     * processus en cours.
     */
    private fun launchSilentInstaller(installer: File) {
        val script = File(tmpDir, "update-${System.currentTimeMillis()}.cmd")
        script.writeText(
            buildString {
                append("@echo off\r\n")
                append("timeout /t 3 /nobreak >nul\r\n")
                // /qn : aucune fenêtre. L'installeur est par-utilisateur, donc
                // aucune élévation n'est demandée.
                append("msiexec /i \"${installer.absolutePath}\" /qn /norestart\r\n")
                append("start \"\" \"${executablePath()}\"\r\n")
                append("del \"%~f0\"\r\n")
            }
        )
        ProcessBuilder("cmd", "/c", "start", "", "/min", script.absolutePath)
            .directory(tmpDir)
            .start()
    }

    /**
     * Chemin réel de l'exécutable en cours.
     *
     * `appFile` le déduisait du répertoire courant du processus, qui n'a aucune
     * raison d'être celui de l'installation : la relance après mise à jour
     * pointait alors sur un fichier inexistant, et l'application ne revenait
     * jamais.
     */
    private fun executablePath(): String =
        ProcessHandle.current().info().command().orElse(null)
            ?: appFile.absolutePath
}
