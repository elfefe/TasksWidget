package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.ClaudeCode
import com.elfefe.common.controller.ClaudeSessions
import com.elfefe.common.controller.MarkdownVisualTransformation
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import com.elfefe.common.model.ThemeColors
import kotlinx.coroutines.delay

/**
 * Carte d'une session Claude Code en cours, volontairement **minimale** : le nom
 * de la session (celui du contrôle à distance quand elle en a un), une icône
 * d'état, un bouton d'édition. Rien d'autre — la conversation se lit dans
 * l'infobulle de l'icône d'état ou dans sa fenêtre dédiée.
 *
 * Le bouton d'édition déplie un éditeur markdown dont le contenu part dans la
 * vraie session du terminal (voir `ClaudeInject`).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClaudeTaskCard(task: Task, windowInteractions: WindowInteractions) {
    val colors = Tasks.Configs.configs.themeColors
    val sessionId = task.claudeSessionId
    val session = ClaudeSessions.running.firstOrNull { it.sessionId == sessionId }

    val name = (session?.name ?: task.title).ifBlank { "Claude Code" }
    val editing = ClaudeSessions.editing == sessionId
    val manager = remember(sessionId) { ClaudeSessions.editor(sessionId) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        backgroundColor = colors.background,
        elevation = 4.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    name,
                    color = colors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                StatusIcon(sessionId, session)
                Spacer(Modifier.width(8.dp))
                Icon(
                    painterResource(if (editing) "edit_off.svg" else "edit.svg"),
                    contentDescription = "Écrire à la session",
                    tint = if (editing) colors.primary else colors.onBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(16.dp)
                        .clickable { ClaudeSessions.toggleEditing(sessionId) }
                )
            }

            AnimatedVisibility(
                visible = editing,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(4.dp))
                    // Barre markdown des tâches : mêmes gestes pour écrire à une
                    // session que pour écrire une description.
                    Editor(manager, windowInteractions)
                    MessageField(session, manager, colors)
                }
            }

            ClaudeSessions.errors[sessionId]?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, color = Color(0xFFCC5555), fontSize = 10.sp)
            }
        }
    }
}

/**
 * Icône d'état : survolée, elle montre les réponses en infobulle ; cliquée, elle
 * ouvre la fenêtre de conversation. Les deux fenêtres sont ouvertes par la pile,
 * qui seule connaît la position de la carte à l'écran.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun StatusIcon(sessionId: String, session: ClaudeCode.RunningSession?) {
    val colors = Tasks.Configs.configs.themeColors
    val busy = session?.busy == true
    val waiting = session?.waiting == true
    val color = when {
        ClaudeSessions.recentlySent(sessionId) -> Color(0xFF3FB950)
        busy -> Color(0xFF3FB950)
        waiting -> Color(0xFFD9A441)
        // Ouverte et prête : même vert, en anneau. Le gris d'avant la rangeait
        // visuellement du côté des sessions mortes.
        session != null -> Color(0xFF3FB950)
        else -> colors.onBackground.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .clickable {
                ClaudeSessions.opened = if (ClaudeSessions.opened == sessionId) null else sessionId
            }
            .onPointerEvent(PointerEventType.Enter) { ClaudeSessions.hovered = sessionId }
            .onPointerEvent(PointerEventType.Exit) {
                if (ClaudeSessions.hovered == sessionId) ClaudeSessions.hovered = null
            },
        contentAlignment = Alignment.Center
    ) {
        StatusDot(
            color = color,
            // Une session ouverte mais au repos se distingue par sa **forme**,
            // un anneau : en jouant sur la seule opacité, elle devenait
            // indiscernable d'une session éteinte.
            filled = session == null || busy || waiting || ClaudeSessions.recentlySent(sessionId),
            pulsing = busy
        )
    }
}

/**
 * Pastille d'état. L'animation porte sur un halo autour de la pastille, jamais
 * sur la pastille elle-même : en faisant varier l'opacité du disque, une session
 * en plein travail passait la moitié du temps pour éteinte — c'est précisément
 * ce qui faisait croire à des sessions inactives.
 */
@Composable
private fun StatusDot(color: Color, filled: Boolean, pulsing: Boolean) {
    val halo by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    Canvas(Modifier.size(18.dp)) {
        val core = 4.dp.toPx()
        if (pulsing) {
            drawCircle(
                color = color.copy(alpha = 0.30f * (1f - halo) + 0.10f),
                radius = core + (5.dp.toPx() * halo)
            )
        }
        if (filled) drawCircle(color = color, radius = core)
        else drawCircle(color = color, radius = core, style = Stroke(width = 1.6.dp.toPx()))
    }
}

/** Champ de saisie du message et son bouton d'envoi. */
@Composable
private fun MessageField(
    session: ClaudeCode.RunningSession?,
    manager: TaskCardManager,
    colors: ThemeColors
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = manager.description,
            onValueChange = {
                manager.selections.add(it.selection)
                manager.description = it
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 28.dp, max = 140.dp)
                .background(colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                .padding(6.dp, 5.dp),
            textStyle = TextStyle(color = colors.onBackground, fontSize = 11.sp),
            cursorBrush = SolidColor(colors.onBackground),
            visualTransformation = MarkdownVisualTransformation()
        )
        Spacer(Modifier.width(4.dp))
        val canSend = manager.description.text.isNotBlank() && session != null
        Icon(
            Icons.Default.Send,
            contentDescription = "Envoyer à la session",
            tint = if (canSend) colors.primary else colors.onBackground.copy(alpha = 0.3f),
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = canSend) { session?.let { ClaudeSessions.send(it) } }
                .padding(3.dp)
        )
    }
}

/**
 * Panneau « Session Claude » du menu « + » : état du compte connecté et
 * ouverture d'une session en contrôle à distance, qui rejoindra la pile toute
 * seule dès que le CLI l'aura déclarée.
 */
@Composable
fun ClaudeLaunchPanel(colors: ThemeColors, onLaunched: () -> Unit) {
    var account by remember { mutableStateOf(ClaudeCode.account()) }
    var prompt by remember { mutableStateOf("") }
    var cwd by remember { mutableStateOf(System.getProperty("user.home") ?: "") }

    LaunchedEffect(Unit) {
        while (true) {
            account = ClaudeCode.account()
            delay(3000)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background, RoundedCornerShape(5.dp))
            .padding(8.dp)
    ) {
        AccountRow(account, colors) { ClaudeCode.login() }
        if (account.connected) {
            Spacer(Modifier.height(6.dp))
            LabeledField("Premier message (facultatif)", prompt, colors, { prompt = it }, singleLine = false)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { LabeledField("Dossier", cwd, colors, { cwd = it }, singleLine = true) }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ouvrir",
                    color = colors.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary)
                        .clickable {
                            ClaudeCode.launch(prompt, cwd, remoteControl = true)
                            prompt = ""
                            onLaunched()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountRow(account: ClaudeCode.Account, colors: ThemeColors, onConnect: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (account.connected && !account.expired) {
            Dot(Color(0xFF3FB950))
            Spacer(Modifier.width(6.dp))
            Text(
                "Connecté" + if (account.plan.isNotBlank()) " · ${account.plan}" else "",
                color = colors.onBackground, fontSize = 11.sp
            )
        } else {
            Dot(Color(0xFFCC5555))
            Spacer(Modifier.width(6.dp))
            Text(
                if (account.expired) "Session expirée" else "Compte non connecté",
                color = colors.onBackground, fontSize = 11.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Se connecter",
                color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onConnect() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    colors: ThemeColors,
    onValue: (String) -> Unit,
    singleLine: Boolean
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = colors.onBackground.copy(alpha = 0.5f), fontSize = 9.sp)
        BasicTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                .padding(6.dp, 4.dp),
            textStyle = TextStyle(color = colors.onBackground, fontSize = 11.sp),
            singleLine = singleLine,
            cursorBrush = SolidColor(colors.onBackground)
        )
    }
}

/** Pastille simple, pour l'état du compte dans le panneau de lancement. */
@Composable
internal fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}
