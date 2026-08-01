package com.elfefe.common.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.BufferedWriter
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Sessions Claude Code **pilotées par le widget** : le CLI est lancé en mode SDK
 * (`-p --input-format stream-json --output-format stream-json`), le widget écrit
 * les messages de l'utilisateur sur stdin et lit les événements sur stdout. On
 * peut ainsi dialoguer avec Claude et voir ses réponses en direct.
 *
 * Étape 1 : dialogue (messages + réponses) + liste des slash-commands. Les
 * questions/permissions (vue latérale) et le fork « Reprendre ici » viendront
 * ensuite.
 */
object ClaudePilot {
    val sessions = mutableStateMapOf<String, ClaudeSession>()

    /** Session de la tâche active dont la fenêtre latérale est ouverte. */
    var active by mutableStateOf<String?>(null)

    fun start(prompt: String, cwd: String, title: String = "Session", resumeId: String? = null): ClaudeSession {
        val id = UUID.randomUUID().toString()
        val session = ClaudeSession(id, cwd.ifBlank { System.getProperty("user.home") }, title)
        sessions[id] = session
        session.launch(prompt, resumeId)
        active = id
        return session
    }

    /** Reprend une session existante en la forkant dans une session pilotée. */
    fun resume(sessionId: String, cwd: String, title: String): ClaudeSession =
        start(prompt = "", cwd = cwd, title = title, resumeId = sessionId)

    fun get(id: String): ClaudeSession? = sessions[id]

    fun close(id: String) {
        sessions.remove(id)?.stop()
        if (active == id) active = null
    }
}

enum class ChatRole { USER, ASSISTANT, TOOL, THINKING, SYSTEM }

data class ChatMsg(val role: ChatRole, val text: String, val id: String = UUID.randomUUID().toString())

enum class SessionStatus { STARTING, IDLE, THINKING, WORKING, ENDED }

class ClaudeSession(val id: String, val cwd: String, initialTitle: String = "Session") {
    private val gson = Gson()

    val messages = mutableStateListOf<ChatMsg>()
    var status by mutableStateOf(SessionStatus.STARTING)
        private set
    var sessionId by mutableStateOf("")
        private set
    var slashCommands by mutableStateOf<List<String>>(emptyList())
        private set
    var title by mutableStateOf(initialTitle)
        private set

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var lastAssistantId: String? = null

    fun launch(initialPrompt: String, resumeId: String? = null) {
        runCatching {
            val args = mutableListOf(
                "claude", "-p",
                "--input-format", "stream-json",
                "--output-format", "stream-json",
                "--verbose",
                "--permission-mode", "bypassPermissions",
                "--include-partial-messages"
            )
            // Reprise : on forke la session existante pour ne pas entrer en
            // conflit avec celle qui tourne éventuellement dans un terminal.
            if (!resumeId.isNullOrBlank()) {
                args.add("--resume"); args.add(resumeId); args.add("--fork-session")
            }
            val pb = ProcessBuilder(args).directory(File(cwd))
            pb.environment()["CLAUDE_CODE_ENTRYPOINT"] = "sdk-taskswidget"
            val proc = pb.start()
            process = proc
            writer = proc.outputStream.bufferedWriter()

            thread(isDaemon = true, name = "claude-$id") {
                runCatching {
                    proc.inputStream.bufferedReader().forEachLine { handle(it) }
                }.onFailure { log(it.stackTraceToString()) }
                status = SessionStatus.ENDED
            }

            if (initialPrompt.isNotBlank()) send(initialPrompt)
            else status = SessionStatus.IDLE
        }.onFailure {
            log(it.stackTraceToString())
            status = SessionStatus.ENDED
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMsg(ChatRole.USER, text))
        status = SessionStatus.THINKING
        lastAssistantId = null
        runCatching {
            val msg = JsonObject().apply {
                addProperty("type", "user")
                add("message", JsonObject().apply {
                    addProperty("role", "user")
                    add("content", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", text)
                        })
                    })
                })
            }
            writer?.apply {
                write(gson.toJson(msg))
                newLine()
                flush()
            }
        }.onFailure { log(it.stackTraceToString()) }
    }

    fun stop() {
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        status = SessionStatus.ENDED
    }

    private fun handle(line: String) {
        val obj = runCatching { gson.fromJson(line, JsonObject::class.java) }.getOrNull() ?: return
        when (obj.get("type")?.asString) {
            "system" -> when (obj.get("subtype")?.asString) {
                "init" -> {
                    sessionId = obj.get("session_id")?.asString ?: ""
                    slashCommands = obj.getAsJsonArray("slash_commands")?.mapNotNull { it.asString } ?: emptyList()
                    if (status == SessionStatus.STARTING) status = SessionStatus.IDLE
                }
                "thinking_tokens" -> status = SessionStatus.THINKING
            }
            "assistant" -> {
                val message = obj.getAsJsonObject("message") ?: return
                val msgId = message.get("id")?.asString ?: ""
                val content = message.getAsJsonArray("content") ?: return
                val text = StringBuilder()
                content.forEach { element ->
                    val part = element.asJsonObject
                    when (part.get("type")?.asString) {
                        "text" -> part.get("text")?.asString?.let { text.append(it) }
                        "tool_use" -> {
                            val name = part.get("name")?.asString ?: "outil"
                            messages.add(ChatMsg(ChatRole.TOOL, name + toolSummary(part)))
                        }
                    }
                }
                if (text.isNotBlank()) upsertAssistant(msgId, text.toString())
                status = SessionStatus.WORKING
            }
            "result" -> status = SessionStatus.IDLE
        }
    }

    /** Un même message assistant arrive en plusieurs événements (même id) : on
     * met à jour au lieu de dupliquer. */
    private fun upsertAssistant(msgId: String, text: String) {
        val last = messages.lastOrNull()
        if (msgId.isNotEmpty() && msgId == lastAssistantId && last?.role == ChatRole.ASSISTANT) {
            messages[messages.lastIndex] = last.copy(text = text)
        } else {
            lastAssistantId = msgId
            messages.add(ChatMsg(ChatRole.ASSISTANT, text))
        }
    }

    private fun toolSummary(part: JsonObject): String {
        val input = part.getAsJsonObject("input") ?: return ""
        val hint = input.get("command")?.asString
            ?: input.get("file_path")?.asString
            ?: input.get("path")?.asString
            ?: input.get("pattern")?.asString
            ?: input.get("description")?.asString
        return if (hint.isNullOrBlank()) "" else " · " + hint.take(80)
    }
}
