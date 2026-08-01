package com.elfefe.common.ui.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.ClaudeCode
import com.elfefe.common.controller.ClaudePilot
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import com.elfefe.common.model.ThemeColors
import kotlinx.coroutines.delay

/**
 * Carte compacte d'une session Claude Code (auto-affichée quand la session est
 * ouverte). Montre en direct le nom, le statut (en cours / en attente /
 * terminée) et la dernière activité, sans surcharge : la connexion et le
 * lancement se font depuis le menu « + ».
 */
@Composable
fun ClaudeTaskCard(task: Task) {
    val colors = Tasks.Configs.configs.themeColors

    var state by remember { mutableStateOf<ClaudeCode.SessionState?>(null) }
    var live by remember { mutableStateOf<ClaudeCode.RunningSession?>(null) }

    LaunchedEffect(task.claudeSessionId, task.claudeCwd) {
        while (true) {
            state = if (task.claudeSessionId.isNotBlank())
                ClaudeCode.sessionState(task.claudeCwd, task.claudeSessionId)
            else ClaudeCode.latestSession(task.claudeCwd.ifBlank { null })
            live = if (task.claudeSessionId.isNotBlank())
                ClaudeCode.runningSessions().firstOrNull { it.sessionId == task.claudeSessionId }
            else null
            delay(2000)
        }
    }

    val busy = live?.busy == true
    val open = live != null
    val name = (live?.name ?: state?.title ?: task.title).ifBlank { "Claude Code" }
    val project = (live?.cwd ?: state?.cwd ?: task.claudeCwd)
        .substringAfterLast('\\').substringAfterLast('/')
    val activity = state?.lastActivity.orEmpty()
    val activityKind = state?.activity ?: ClaudeCode.Activity.NONE

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
                when {
                    busy -> PulsingDot(Color(0xFF3FB950))
                    open -> Dot(Color(0xFFD9A441))
                    else -> Dot(colors.onBackground.copy(alpha = 0.4f))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    when {
                        busy -> "en cours"
                        open -> "en attente"
                        else -> "terminée"
                    },
                    color = if (busy) Color(0xFF3FB950) else colors.onBackground.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (activity.isNotBlank() || project.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    activityIcon(activityKind)?.let {
                        Icon(it, null, tint = colors.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        activity,
                        color = colors.onBackground.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (project.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.FolderOpen, null,
                            tint = colors.onBackground.copy(alpha = 0.45f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(project, color = colors.onBackground.copy(alpha = 0.5f), fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun activityIcon(kind: ClaudeCode.Activity): androidx.compose.ui.graphics.vector.ImageVector? = when (kind) {
    ClaudeCode.Activity.TOOL -> Icons.Default.Build
    ClaudeCode.Activity.TEXT -> Icons.Default.ChatBubbleOutline
    ClaudeCode.Activity.RESULT -> Icons.Default.SubdirectoryArrowRight
    ClaudeCode.Activity.NONE -> null
}

/**
 * Panneau « Ouvrir une session Claude » du menu « + » : état du compte (avec
 * connexion si besoin) et lancement d'une nouvelle session. La session lancée
 * apparaît ensuite toute seule comme carte.
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
            LabeledField("Prompt de la nouvelle session", prompt, colors, { prompt = it }, singleLine = false)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { LabeledField("Dossier", cwd, colors, { cwd = it }, singleLine = true) }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Lancer",
                    color = colors.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary.copy(alpha = if (prompt.isBlank()) 0.4f else 1f))
                        .clickable(enabled = prompt.isNotBlank()) {
                            ClaudePilot.start(prompt, cwd)
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

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
    )
    Box(Modifier.size(8.dp).clip(CircleShape).alpha(alpha).background(color))
}
