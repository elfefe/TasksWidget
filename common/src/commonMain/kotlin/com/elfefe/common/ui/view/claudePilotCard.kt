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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.ChatRole
import com.elfefe.common.controller.ClaudePilot
import com.elfefe.common.controller.SessionStatus
import com.elfefe.common.model.Task
import com.elfefe.common.model.ThemeColors
import com.elfefe.common.controller.Tasks

/**
 * Carte d'une session Claude Code **pilotée** : fil de discussion, saisie
 * markdown + envoi, et bouton de slash-commands.
 */
@Composable
fun ClaudePilotCard(task: Task) {
    val colors = Tasks.Configs.configs.themeColors
    val session = ClaudePilot.get(task.claudeSessionId) ?: return

    var input by remember { mutableStateOf("") }
    var showCommands by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    // Défilement auto vers le bas à chaque nouveau message.
    LaunchedEffect(session.messages.size) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        backgroundColor = colors.background,
        elevation = 4.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            // En-tête : icône + titre + statut + fermer.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, null, tint = colors.onBackground, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (session.title.isBlank()) "Claude" else session.title,
                    color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(session.status, colors)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close, null,
                    tint = colors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp).clip(CircleShape).clickable {
                        ClaudePilot.close(task.claudeSessionId)
                    }
                )
            }

            Spacer(Modifier.height(6.dp))

            // Fil de discussion (hauteur bornée + défilement interne).
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 260.dp)
                    .verticalScroll(scroll)
            ) {
                session.messages.forEach { msg -> MessageRow(msg, colors) }
            }

            Spacer(Modifier.height(6.dp))

            // Liste des slash-commands (repliable).
            AnimatedVisibility(visible = showCommands) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .verticalScroll(rememberScrollState())
                        .background(colors.onBackground.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    session.slashCommands.forEach { cmd ->
                        Text(
                            "/$cmd",
                            color = colors.onBackground,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    input = "/$cmd "
                                    showCommands = false
                                }
                                .padding(vertical = 3.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            // Barre de saisie : bouton commandes + champ markdown + envoyer.
            Row(verticalAlignment = Alignment.Bottom) {
                Icon(
                    Icons.Default.Terminal, "Commandes",
                    tint = if (showCommands) colors.primary else colors.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showCommands = !showCommands }
                        .padding(2.dp)
                )
                Spacer(Modifier.width(4.dp))
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 24.dp, max = 100.dp)
                        .background(colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                        .padding(6.dp, 5.dp),
                    textStyle = TextStyle(color = colors.onBackground, fontSize = 11.sp),
                    cursorBrush = SolidColor(colors.onBackground)
                )
                Spacer(Modifier.width(4.dp))
                val canSend = input.isNotBlank() && session.status != SessionStatus.ENDED
                Icon(
                    Icons.Default.Send, "Envoyer",
                    tint = if (canSend) colors.primary else colors.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = canSend) {
                            session.send(input.trim())
                            input = ""
                        }
                        .padding(3.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageRow(msg: com.elfefe.common.controller.ChatMsg, colors: ThemeColors) {
    when (msg.role) {
        ChatRole.USER -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.End) {
            Text(
                msg.text,
                color = colors.onPrimary,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(colors.primary, RoundedCornerShape(6.dp))
                    .padding(6.dp, 4.dp)
            )
        }
        ChatRole.ASSISTANT -> Text(
            msg.text,
            color = colors.onBackground,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        ChatRole.TOOL -> Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, null, tint = colors.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(msg.text, color = colors.onBackground.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
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
