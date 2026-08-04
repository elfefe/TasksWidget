package com.elfefe.common.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.elfefe.common.controller.ClaudeCode
import com.elfefe.common.controller.ClaudeSessions
import com.elfefe.common.controller.CrashWindow
import com.elfefe.common.controller.MarkdownVisualTransformation
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.ThemeColors
import com.elfefe.common.ui.theme.TasksTheme
import kotlinx.coroutines.delay

/** Largeur commune aux deux fenêtres de session, en dp. */
const val SESSION_WINDOW_WIDTH = 360f

/**
 * Sonde le fil d'une session tant que la fenêtre qui l'affiche est là. Le
 * transcript est la seule source qui vaille : il est écrit par le CLI, que la
 * session ait été lancée par le widget ou ouverte à la main dans un terminal.
 */
@Composable
private fun conversationOf(sessionId: String, cwd: String, maxBytes: Int): State<List<ClaudeCode.Line>> {
    val lines = remember(sessionId) { mutableStateOf(emptyList<ClaudeCode.Line>()) }
    LaunchedEffect(sessionId, cwd) {
        while (true) {
            lines.value = runCatching { ClaudeCode.conversation(cwd, sessionId, maxBytes) }
                .getOrDefault(emptyList())
            delay(1500)
        }
    }
    return lines
}

/**
 * Infobulle des réponses, ouverte au survol de l'icône d'état. Volontairement
 * non focalisable : elle ne doit ni voler le curseur, ni passer devant la saisie
 * en cours. Elle se contente des dernières prises de parole.
 */
@Composable
fun SessionTooltipWindow(sessionId: String, xDp: Float, yDp: Float) {
    val colors = Tasks.Configs.configs.themeColors
    val session = ClaudeSessions.running.firstOrNull { it.sessionId == sessionId } ?: return
    val lines by conversationOf(sessionId, session.cwd, maxBytes = 64 * 1024)

    // Les derniers échanges, réponses de Claude en priorité : c'est ce qu'on
    // vient lire d'un coup d'œil.
    val recent = remember(lines) {
        lines.filter { it.speaker != ClaudeCode.Speaker.TOOL }.takeLast(4)
    }

    val state = rememberWindowState(
        position = WindowPosition.Absolute(xDp.dp, yDp.dp),
        size = DpSize(SESSION_WINDOW_WIDTH.dp, 200.dp)
    )
    LaunchedEffect(xDp, yDp) { state.position = WindowPosition.Absolute(xDp.dp, yDp.dp) }

    CrashWindow(
        onCloseRequest = { ClaudeSessions.hovered = null },
        state = state,
        visible = true,
        title = "Claude - aperçu",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true
    ) {
        TasksTheme {
            Card(
                Modifier.fillMaxSize().padding(4.dp),
                backgroundColor = colors.background,
                elevation = 6.dp
            ) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Text(
                        session.name.ifBlank { "Claude" },
                        color = colors.onBackground.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    if (recent.isEmpty()) {
                        Text(
                            "Pas encore de réponse.",
                            color = colors.onBackground.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    } else {
                        recent.forEach { line ->
                            if (line.speaker == ClaudeCode.Speaker.USER) Text(
                                line.text.replace('\n', ' ').take(300),
                                color = colors.onBackground.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) else MarkdownText(
                                markdown = line.text.take(600),
                                colors = colors,
                                maxLines = 4,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fenêtre dédiée à une session : le fil complet, et de quoi répondre sans passer
 * par la carte. Elle est volontairement indépendante de la pile — celle-ci se
 * replie dès que la souris s'en éloigne, ce qui emporterait la conversation
 * qu'on est en train de lire.
 */
@Composable
fun SessionConversationWindow(sessionId: String, xDp: Float, yDp: Float) {
    val colors = Tasks.Configs.configs.themeColors
    val session = ClaudeSessions.running.firstOrNull { it.sessionId == sessionId }
        ?: run { ClaudeSessions.opened = null; return }
    val lines by conversationOf(sessionId, session.cwd, maxBytes = 192 * 1024)

    val state = rememberWindowState(
        position = WindowPosition.Absolute(xDp.dp, yDp.dp),
        size = DpSize(SESSION_WINDOW_WIDTH.dp, 480.dp)
    )
    LaunchedEffect(xDp, yDp) { state.position = WindowPosition.Absolute(xDp.dp, yDp.dp) }

    CrashWindow(
        onCloseRequest = { ClaudeSessions.opened = null },
        state = state,
        visible = true,
        title = "Claude - conversation",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        TasksTheme {
            Card(
                Modifier.fillMaxSize().padding(4.dp),
                backgroundColor = colors.background,
                elevation = 8.dp
            ) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy, null,
                            tint = colors.onBackground, modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            session.name.ifBlank { "Claude" },
                            color = colors.onBackground, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            statusLabel(session),
                            color = if (session.busy) Color(0xFF3FB950)
                            else colors.onBackground.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Close, "Fermer",
                            tint = colors.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp).clip(CircleShape)
                                .clickable { ClaudeSessions.opened = null }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    ConversationThread(lines, colors, Modifier.weight(1f))
                    Spacer(Modifier.height(4.dp))
                    Reply(session, colors)
                }
            }
        }
    }
}

@Composable
private fun ConversationThread(lines: List<ClaudeCode.Line>, colors: ThemeColors, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }

    Column(modifier.fillMaxWidth().verticalScroll(scroll)) {
        if (lines.isEmpty()) {
            Text(
                "Rien à afficher pour l'instant.",
                color = colors.onBackground.copy(alpha = 0.5f), fontSize = 11.sp
            )
        }
        lines.forEach { line ->
            when (line.speaker) {
                ClaudeCode.Speaker.USER -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        line.text, color = colors.onPrimary, fontSize = 11.sp,
                        modifier = Modifier
                            .background(colors.primary, RoundedCornerShape(6.dp))
                            .padding(6.dp, 4.dp)
                    )
                }

                // Les réponses arrivent en markdown : titres, listes, blocs de
                // code et liens sont rendus, pas donnés à lire en balisage.
                ClaudeCode.Speaker.CLAUDE -> MarkdownText(
                    markdown = line.text,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )

                ClaudeCode.Speaker.TOOL -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Build, null,
                        tint = colors.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        line.text, color = colors.onBackground.copy(alpha = 0.6f),
                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Réponse rapide depuis la fenêtre, sur le même brouillon que la carte. */
@Composable
private fun Reply(session: ClaudeCode.RunningSession, colors: ThemeColors) {
    val manager = remember(session.sessionId) { ClaudeSessions.editor(session.sessionId) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = manager.description,
            onValueChange = {
                manager.selections.add(it.selection)
                manager.description = it
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 28.dp, max = 120.dp)
                .background(colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                .padding(6.dp, 5.dp),
            textStyle = LocalTextStyle.current.copy(color = colors.onBackground, fontSize = 11.sp),
            cursorBrush = SolidColor(colors.onBackground),
            // Même rendu que dans la carte : écrire à une session ne doit pas
            // dépendre de l'endroit d'où on écrit.
            visualTransformation = MarkdownVisualTransformation()
        )
        Spacer(Modifier.width(4.dp))
        val canSend = manager.description.text.isNotBlank()
        Icon(
            Icons.Default.Send, "Envoyer",
            tint = if (canSend) colors.primary else colors.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                .clickable(enabled = canSend) { ClaudeSessions.send(session) }
                .padding(3.dp)
        )
    }
}

private fun statusLabel(session: ClaudeCode.RunningSession): String = when {
    session.busy -> "en cours"
    session.waiting -> session.waitingFor.ifBlank { "en attente" }
    else -> "prête"
}
