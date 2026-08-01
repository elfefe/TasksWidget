package com.elfefe.common.controller

import androidx.compose.ui.res.useResource
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import com.google.cloud.logging.Payload.StringPayload
import com.google.cloud.logging.Severity
import java.util.logging.Logger

private val logging: Logging by lazy {
    LoggingOptions.newBuilder()
        .setCredentials(
            useResource("taskwidget-logger-1.json") { resource ->
                ServiceAccountCredentials.fromStream(resource)
            }
        )
        .setProjectId("taskwidget-b17c3")
        .build()
        .service
}

fun sendLogs(content: String, level: Severity = Severity.INFO) {
    try {
        println("Sending log to GCP")
        val entry = LogEntry.newBuilder(StringPayload.of(content))
            .setSeverity(level)
            .setLogName("TasksWidgetLog")
            .build()
        logging.write(listOf(entry))
    } catch (e: Exception) {
        // Envoi distant best-effort : on n'écrit PAS l'échec dans app.log, sinon
        // il masque les vrais messages (le fichier de log est écrit par log()).
        println(e.stackTraceToString())
    }
}

fun Any.log(message: Any, level: Severity = Severity.INFO) {

    println(message)
    // On écrit le message dans app.log directement : l'envoi distant peut
    // échouer (ressource GCP absente) et ne doit pas nous priver de la trace.
    runCatching { logsFile.appendText("[${java.util.Date()}] $message\n") }
    Logger.getLogger(this::class.java.name).run {
        when(level) {
            Severity.DEBUG -> fine(message.toString())
            Severity.INFO -> info(message.toString())
            Severity.WARNING -> warning(message.toString())
            Severity.ERROR -> severe(message.toString())
            Severity.CRITICAL -> severe("CRITICAL: $message")
            else -> info(message.toString())
        }
    }
    sendLogs(message.toString(), level)
}
