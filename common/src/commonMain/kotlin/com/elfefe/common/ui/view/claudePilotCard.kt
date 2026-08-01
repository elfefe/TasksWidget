package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
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
import com.elfefe.common.controller.ChatMsg
import com.elfefe.common.controller.ChatRole
import com.elfefe.common.controller.ClaudePilot
import com.elfefe.common.controller.ClaudeSession
import com.elfefe.common.controller.CrashWindow
import com.elfefe.common.controller.SessionStatus
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import com.elfefe.common.model.ThemeColors
import com.elfefe.common.ui.theme.TasksTheme

/**
 * Carte **concise** d'une session Claude pilotée : icône + nom + statut + la
 * dernière ligne. Un clic ouvre la fenêtre latérale (fil complet + réponse).
 */
@Composable
fun ClaudePilotCard(task: Task) {
    val colors = Tasks.Configs.configs.themeColors
    val session = ClaudePilot.get(task.claudeSessionId) ?: return
    val last = session.messages.lastOrNull { it.role == ChatRole.ASSISTANT || it.role == ChatRole.TOOL }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable {
                ClaudePilot.active = if (ClaudePilot.active == task.claudeSessionId) null else task.claudeSessionId
            },
        backgroundColor = colors.background,
        elevation = 4.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, null, tint = colors.onBackground, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    session.title.ifBlank { "Claude" },
                    color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                StatusBadge(session.status, colors)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close, null, tint = colors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp).clip(CircleShape).clickable { ClaudePilot.close(task.claudeSessionId) }
                )
            }
            last?.let {
                Spacer(Modifier.height(3.dp))
                Text(
                    it.text, color = colors.onBackground.copy(alpha = 0.8f), fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Fenêtre latérale, juste à côté de la tâche active : fil complet + réponse +
 * slash-commands. [xDp]/[yDp] sont calculés par la pile pour la placer contre
 * la carte active.
 */
@Composable
fun PilotSideWindow(xDp: Float, yDp: Float) {
    val activeId = ClaudePilot.active ?: return
    val session = ClaudePilot.get(activeId) ?: run { ClaudePilot.active = null; return }
    val colors = Tasks.Configs.configs.themeColors

    val state = rememberWindowState(
        position = WindowPosition.Absolute(xDp.dp, yDp.dp),
        size = DpSize(340.dp, 460.dp)
    )
    LaunchedEffect(xDp, yDp) { state.position = WindowPosition.Absolute(xDp.dp, yDp.dp) }

    CrashWindow(
        onCloseRequest = { ClaudePilot.active = null },
        state = state,
        visible = true,
        title = "Claude",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        TasksTheme {
            Card(Modifier.fillMaxSize().padding(4.dp), backgroundColor = colors.background, elevation = 6.dp) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, null, tint = colors.onBackground, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            session.title.ifBlank { "Claude" }, color = colors.onBackground,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                        StatusBadge(session.status, colors)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close, null, tint = colors.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp).clip(CircleShape).clickable { ClaudePilot.active = null }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    PilotThread(session, colors, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PilotThread(session: ClaudeSession, colors: ThemeColors, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var showCommands by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    LaunchedEffect(session.messages.size) { scroll.animateScrollTo(scroll.maxValue) }

    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll)) {
            session.messages.forEach { MessageRow(it, colors) }
        }

        AnimatedVisibility(visible = showCommands) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState())
                    .background(colors.onBackground.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(4.dp)
            ) {
                session.slashCommands.forEach { cmd ->
                    Text(
                        "/$cmd", color = colors.onBackground, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { input = "/$cmd "; showCommands = false }
                            .padding(vertical = 3.dp, horizontal = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Icon(
                Icons.Default.Terminal, "Commandes",
                tint = if (showCommands) colors.primary else colors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).clickable { showCommands = !showCommands }.padding(2.dp)
            )
            Spacer(Modifier.width(4.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).heightIn(min = 24.dp, max = 110.dp)
                    .background(colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(4.dp)).padding(6.dp, 5.dp),
                textStyle = TextStyle(color = colors.onBackground, fontSize = 11.sp),
                cursorBrush = SolidColor(colors.onBackground)
            )
            Spacer(Modifier.width(4.dp))
            val canSend = input.isNotBlank() && session.status != SessionStatus.ENDED
            Icon(
                Icons.Default.Send, "Envoyer",
                tint = if (canSend) colors.primary else colors.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = canSend) { session.send(input.trim()); input = "" }.padding(3.dp)
            )
        }
    }
}

@Composable
private fun MessageRow(msg: ChatMsg, colors: ThemeColors) {
    when (msg.role) {
        ChatRole.USER -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.End) {
            Text(
                msg.text, color = colors.onPrimary, fontSize = 11.sp,
                modifier = Modifier.background(colors.primary, RoundedCornerShape(6.dp)).padding(6.dp, 4.dp)
            )
        }
        ChatRole.ASSISTANT -> Text(
            msg.text, color = colors.onBackground, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        ChatRole.TOOL -> Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, null, tint = colors.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(msg.text, color = colors.onBackground.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        else -> Text(msg.text, color = colors.onBackground.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
private fun StatusBadge(status: SessionStatus, colors: ThemeColors) {
    val (label, color) = when (status) {
        SessionStatus.STARTING -> "démarrage" to Color(0xFFD9A441)
        SessionStatus.THINKING -> "réfléchit" to Color(0xFF3FB950)
        SessionStatus.WORKING -> "travaille" to Color(0xFF3FB950)
        SessionStatus.IDLE -> "prêt" to colors.onBackground.copy(alpha = 0.6f)
        SessionStatus.ENDED -> "terminée" to colors.onBackground.copy(alpha = 0.4f)
    }
    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(4.dp))
    Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
}
