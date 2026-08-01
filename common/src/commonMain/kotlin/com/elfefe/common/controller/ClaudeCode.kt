package com.elfefe.common.controller

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/**
 * Passerelle vers l'installation locale de Claude Code : état du compte
 * connecté (lecture de ~/.claude/.credentials.json, jamais le jeton lui-même),
 * lancement d'une session, et lecture de l'état d'une session en cours à partir
 * de son transcript ~/.claude/projects/<projet>/<session>.jsonl.
 */
object ClaudeCode {
    private val gson = Gson()
    val claudeDir = File(System.getProperty("user.home"), ".claude")
    private val projectsDir = File(claudeDir, "projects")
    private val credentialsFile = File(claudeDir, ".credentials.json")

    /** Une session est considérée « en cours » si son transcript a bougé récemment. */
    private const val RUNNING_WINDOW_MS = 12_000L

    data class Account(
        val connected: Boolean,
        val plan: String,
        val expired: Boolean
    )

    /** État du compte Claude Code déjà connecté sur cette machine. */
    fun account(): Account {
        if (!credentialsFile.exists()) return Account(false, "", false)
        return runCatching {
            val root = gson.fromJson(credentialsFile.readText(), JsonObject::class.java)
            val oauth = root.getAsJsonObject("claudeAiOauth")
                ?: return Account(false, "", false)
            val hasToken = oauth.has("accessToken") &&
                    oauth.get("accessToken").asString.isNotBlank()
            val expiresAt = oauth.get("expiresAt")?.asLong ?: 0L
            val expired = expiresAt in 1 until System.currentTimeMillis()
            val plan = oauth.get("subscriptionType")?.asString ?: ""
            Account(connected = hasToken, plan = plan, expired = expired)
        }.getOrElse { Account(false, "", false) }
    }

    /** Le CLI `claude` est-il résoluble sur le PATH ? */
    fun available(): Boolean = runCatching {
        val process = ProcessBuilder("cmd", "/c", "where claude")
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        process.exitValue() == 0
    }.getOrDefault(false)

    /** Ouvre un terminal qui lance `claude login` (flux OAuth interactif). */
    fun login() {
        runCatching {
            ProcessBuilder("cmd", "/c", "start", "Claude Code - login", "cmd", "/k", "claude login")
                .start()
        }.onFailure { log(it.stackTraceToString()) }
    }

    /**
     * Ouvre une session Claude Code interactive dans un terminal, dans le
     * dossier [cwd], amorcée avec [prompt]. On passe par un petit script pour
     * éviter l'enfer des échappements. Renvoie l'instant de lancement (pour
     * ensuite rattacher la tâche à la session la plus récente du projet).
     */
    fun launch(prompt: String, cwd: String): Long {
        val startedAt = System.currentTimeMillis()
        runCatching {
            val dir = if (cwd.isNotBlank() && File(cwd).isDirectory) cwd
            else System.getProperty("user.home")
            val script = File(tmpDir, "claude-launch-${UUID.randomUUID()}.cmd")
            script.writeText(
                buildString {
                    append("@echo off\r\n")
                    append("cd /d \"$dir\"\r\n")
                    // Le prompt est passé en argument ; les guillemets internes
                    // sont doublés pour cmd.
                    val safe = prompt.replace("\"", "\"\"")
                    append("claude \"$safe\"\r\n")
                }
            )
            ProcessBuilder("cmd", "/c", "start", "Claude Code", "cmd", "/k", script.absolutePath)
                .start()
        }.onFailure { log(it.stackTraceToString()) }
        return startedAt
    }

    data class RunningSession(
        val sessionId: String,
        val cwd: String,
        val name: String,
        val status: String,   // "busy", "idle"...
        val startedAt: Long,
        val updatedAt: Long,
        val pid: Long,
        val bridged: Boolean = false
    ) {
        val busy: Boolean get() = status.equals("busy", ignoreCase = true)
        val project: String get() = cwd.substringAfterLast('\\').substringAfterLast('/')
    }

    /**
     * Toutes les sessions Claude Code réellement ouvertes : un fichier
     * ~/.claude/sessions/<pid>.json par session, dont on ne garde que celles
     * dont le process (pid) est encore vivant. Le champ `status` (busy/idle)
     * évite d'avoir à deviner l'activité via les transcripts.
     */
    fun runningSessions(): List<RunningSession> {
        val dir = File(claudeDir, "sessions")
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching {
                val o = gson.fromJson(file.readText(), JsonObject::class.java) ?: return@mapNotNull null
                val pid = o.get("pid")?.asLong ?: return@mapNotNull null
                if (!ProcessHandle.of(pid).isPresent) return@mapNotNull null
                val sessionId = o.get("sessionId")?.asString ?: return@mapNotNull null
                val cwd = o.get("cwd")?.asString ?: ""
                val derived = o.get("name")?.asString?.takeIf { it.isNotBlank() } ?: sessionId.take(8)
                // Session en remote-control (bridgée) : on préfère son nom
                // remote-control (le titre IA affiché dans l'app Claude) au nom
                // dérivé « fbou-70 ».
                val bridged = !o.get("bridgeSessionId")?.asString.isNullOrBlank()
                val name = if (bridged) aiTitleOf(cwd, sessionId) ?: derived else derived
                RunningSession(
                    sessionId = sessionId,
                    cwd = cwd,
                    name = name,
                    status = o.get("status")?.asString ?: "",
                    startedAt = o.get("startedAt")?.asLong ?: 0L,
                    updatedAt = o.get("updatedAt")?.asLong ?: 0L,
                    pid = pid,
                    bridged = bridged
                )
            }.getOrNull()
        }.sortedByDescending { it.startedAt } // ordre stable (updatedAt bougerait sans cesse)
    }

    /** Nature de la dernière activité, pour choisir l'icône côté UI. */
    enum class Activity { NONE, TEXT, TOOL, RESULT }

    data class SessionState(
        val sessionId: String,
        val cwd: String,
        val title: String,
        val running: Boolean,
        val lastActivity: String,
        val activity: Activity,
        val lastModified: Long
    )

    /** Encodage d'un chemin en nom de dossier projet, comme Claude Code. */
    fun encodeCwd(cwd: String): String = cwd.replace(Regex("[^A-Za-z0-9]"), "-")

    /** Titre IA (le nom affiché en remote-control), lu en tête du transcript. */
    fun aiTitleOf(cwd: String, sessionId: String): String? {
        val file = File(File(projectsDir, encodeCwd(cwd)), "$sessionId.jsonl")
        if (!file.exists()) return null
        var title: String? = null
        // Tête large : l'ai-title arrive après le contenu de démarrage (hook
        // mémoire ~15-50 Ko), donc au-delà des premières dizaines de Ko.
        readHead(file, 256 * 1024).forEach { line ->
            val obj = parse(line) ?: return@forEach
            when (obj.get("type")?.asString) {
                "ai-title" -> obj.get("aiTitle")?.asString?.let { if (it.isNotBlank()) title = it }
                "agent-name" -> obj.get("agentName")?.asString?.let { if (title.isNullOrBlank()) title = it }
            }
        }
        return title
    }

    /**
     * Session la plus récente à surveiller. Si [cwd] est fourni, on se limite à
     * ce projet ; sinon on prend la session la plus récemment active toutes
     * projets confondus (utile pour « suivre ce que Claude Code fait là »).
     * [after] permet de ne considérer que les sessions créées après un
     * lancement, pour rattacher une tâche fraîchement lancée.
     */
    fun latestSession(cwd: String? = null, after: Long = 0L): SessionState? {
        val dirs = when {
            !cwd.isNullOrBlank() -> listOf(File(projectsDir, encodeCwd(cwd)))
            else -> projectsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        }
        val transcript = dirs
            .flatMap { it.listFiles { f -> f.extension == "jsonl" }?.toList() ?: emptyList() }
            .filter { it.lastModified() >= after }
            .maxByOrNull { it.lastModified() }
            ?: return null
        return readSession(transcript)
    }

    fun sessionState(cwd: String, sessionId: String): SessionState? {
        val file = File(File(projectsDir, encodeCwd(cwd)), "$sessionId.jsonl")
        if (!file.exists()) return null
        return readSession(file)
    }

    private fun readSession(file: File): SessionState {
        val sessionId = file.nameWithoutExtension
        val head = readHead(file, 256 * 1024)
        val tail = readTail(file, 48 * 1024)

        var title = ""
        var cwd = ""
        head.forEach { line ->
            val obj = parse(line) ?: return@forEach
            when (obj.get("type")?.asString) {
                "ai-title" -> obj.get("aiTitle")?.asString?.let { if (it.isNotBlank()) title = it }
                "agent-name" -> obj.get("agentName")?.asString?.let { if (title.isBlank()) title = it }
            }
            obj.get("cwd")?.asString?.let { if (it.isNotBlank()) cwd = it }
        }

        var lastActivity = ""
        var lastKind = Activity.NONE
        var lastCwd = cwd
        tail.forEach { line ->
            val obj = parse(line) ?: return@forEach
            obj.get("cwd")?.asString?.let { if (it.isNotBlank()) lastCwd = it }
            summarize(obj)?.let { (kind, text) -> lastKind = kind; lastActivity = text }
        }

        if (title.isBlank()) title = "Session ${sessionId.take(8)}"
        val running = System.currentTimeMillis() - file.lastModified() < RUNNING_WINDOW_MS
        return SessionState(
            sessionId = sessionId,
            cwd = if (lastCwd.isNotBlank()) lastCwd else cwd,
            title = title,
            running = running,
            lastActivity = lastActivity,
            activity = lastKind,
            lastModified = file.lastModified()
        )
    }

    /** Résumé court (nature + texte) de la dernière activité d'un événement. */
    private fun summarize(obj: JsonObject): Pair<Activity, String>? {
        val type = obj.get("type")?.asString ?: return null
        if (type != "assistant" && type != "user") return null
        val message = obj.getAsJsonObject("message") ?: return null
        val content = message.get("content") ?: return null

        if (content.isJsonPrimitive) {
            val text = content.asString.trim()
            return if (text.isBlank()) null else Activity.TEXT to text.take(120)
        }
        if (!content.isJsonArray) return null

        var text: String? = null
        var tool: String? = null
        var toolResult = false
        content.asJsonArray.forEach { element ->
            val part = element.asJsonObject
            when (part.get("type")?.asString) {
                "text" -> part.get("text")?.asString?.let { if (it.isNotBlank()) text = it }
                "tool_use" -> tool = part.get("name")?.asString
                "tool_result" -> toolResult = true
            }
        }
        return when {
            tool != null -> Activity.TOOL to tool!!
            text != null -> Activity.TEXT to text!!.trim().take(120)
            toolResult -> Activity.RESULT to "résultat d'outil"
            else -> null
        }
    }

    private fun parse(line: String): JsonObject? =
        runCatching { gson.fromJson(line, JsonObject::class.java) }.getOrNull()

    private fun readHead(file: File, bytes: Int): List<String> = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val len = minOf(bytes.toLong(), raf.length())
            val buffer = ByteArray(len.toInt())
            raf.seek(0)
            raf.readFully(buffer)
            String(buffer, Charsets.UTF_8).split('\n').filter { it.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    private fun readTail(file: File, bytes: Int): List<String> = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val len = raf.length()
            val from = maxOf(0L, len - bytes)
            raf.seek(from)
            val buffer = ByteArray((len - from).toInt())
            raf.readFully(buffer)
            String(buffer, Charsets.UTF_8).split('\n')
                .drop(if (from > 0) 1 else 0) // 1re ligne tronquée si on n'est pas au début
                .filter { it.isNotBlank() }
        }
    }.getOrDefault(emptyList())
}
