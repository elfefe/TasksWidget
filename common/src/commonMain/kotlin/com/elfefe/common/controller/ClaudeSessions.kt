package com.elfefe.common.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.elfefe.common.model.Task
import com.elfefe.common.ui.view.TaskCardManager
import kotlin.concurrent.thread

/**
 * État d'interface des sessions Claude Code suivies par le widget : quelle carte
 * a son éditeur déplié, laquelle est survolée (infobulle), laquelle a sa fenêtre
 * de conversation ouverte — et l'envoi lui-même.
 *
 * Cet état vit hors des composables parce que chaque carte est **une fenêtre**
 * séparée : l'infobulle et la fenêtre de conversation sont ouvertes par la pile,
 * pas par la carte, et il leur faut un point de rendez-vous commun.
 */
object ClaudeSessions {

    /**
     * Sessions Claude Code réellement ouvertes sur la machine, relevées en
     * continu par la pile. Source unique : les cartes, l'infobulle et la fenêtre
     * de conversation lisent toutes ici plutôt que de sonder chacune le disque.
     */
    var running by mutableStateOf<List<ClaudeCode.RunningSession>>(emptyList())

    /** Session dont l'icône d'état est survolée : son infobulle est affichée. */
    var hovered by mutableStateOf<String?>(null)

    /** Session dont la fenêtre de conversation est ouverte. */
    var opened by mutableStateOf<String?>(null)

    /** Session dont l'éditeur markdown est déplié dans la carte. */
    var editing by mutableStateOf<String?>(null)

    /** Instant du dernier envoi réussi, par session : accusé de réception bref. */
    val sentAt = mutableStateMapOf<String, Long>()

    /** Dernier échec d'envoi, par session. */
    val errors = mutableStateMapOf<String, String>()

    /**
     * Brouillons de message, un éditeur par session. Ils vivent ici pour
     * survivre au repli de la pile et à la recomposition des fenêtres : un
     * message à moitié écrit ne doit pas disparaître parce que la souris est
     * passée ailleurs.
     */
    private val editors = mutableMapOf<String, TaskCardManager>()

    fun editor(sessionId: String): TaskCardManager =
        editors.getOrPut(sessionId) {
            // La barre markdown est montée d'emblée : la carte ne l'affiche que
            // lorsqu'elle déplie l'éditeur, et la poser ici évite d'écrire cet
            // état depuis la composition, ce que Compose ne pardonne pas.
            TaskCardManager(Task(title = sessionId)).apply { showEditor = true }
        }

    /** Bascule l'éditeur d'une session (un seul ouvert à la fois). */
    fun toggleEditing(sessionId: String) {
        editing = if (editing == sessionId) null else sessionId
    }

    /**
     * Envoie le brouillon à la session. En cas de succès le brouillon est vidé —
     * le message part vraiment dans le terminal, le garder inviterait à le
     * renvoyer deux fois.
     *
     * L'injection laisse à la TUI le temps d'absorber le collage avant de
     * valider : elle se fait donc à l'écart du fil d'interface, qui figerait
     * sinon tout le widget le temps de l'envoi.
     */
    fun send(session: ClaudeCode.RunningSession) {
        val manager = editor(session.sessionId)
        val text = manager.description.text
        if (text.isBlank()) return

        thread(isDaemon = true, name = "claude-send-${session.pid}") {
            val sent = ClaudeInject.send(session.pid, text)
            if (sent) {
                // Ne vider que si rien n'a été retapé entre-temps.
                if (manager.description.text == text) manager.description = TextFieldValue("")
                sentAt[session.sessionId] = System.currentTimeMillis()
                errors.remove(session.sessionId)
            } else {
                errors[session.sessionId] = "Envoi impossible : la console de la session est hors d'atteinte."
            }
        }
    }

    /** L'accusé de réception ne reste affiché que quelques secondes. */
    fun recentlySent(sessionId: String, now: Long = System.currentTimeMillis()): Boolean =
        now - (sentAt[sessionId] ?: 0L) < 3_000L

    /** Oublie tout ce qui concerne une session disparue. */
    fun forget(sessionId: String) {
        editors.remove(sessionId)
        sentAt.remove(sessionId)
        errors.remove(sessionId)
        if (hovered == sessionId) hovered = null
        if (opened == sessionId) opened = null
        if (editing == sessionId) editing = null
    }
}
