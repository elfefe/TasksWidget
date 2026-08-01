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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.ClaudeCode
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Carte d'une tâche de type "claude" : montre l'état du compte Claude Code,
 * permet de le connecter s'il ne l'est pas, de lancer une session avec un
 * prompt, et visualise en direct la session suivie.
 */
@Composable
fun ClaudeTaskCard(task: Task) {
    val scope = rememberCoroutineScope()
    val colors = Tasks.Configs.configs.themeColors

    var title by remember { mutableStateOf(task.title.ifBlank { "Claude Code" }) }
    var prompt by remember { mutableStateOf(task.description) }
    var cwd by remember { mutableStateOf(task.claudeCwd.ifBlank { System.getProperty("user.home") }) }
    var sessionId by remember { mutableStateOf(task.claudeSessionId) }
    var done by remember { mutableStateOf(task.done) }

    var account by remember { mutableStateOf(ClaudeCode.account()) }
    var state by remember { mutableStateOf<ClaudeCode.SessionState?>(null) }

    // Rafraîchissement en direct de l'état du compte et de la session suivie.
    LaunchedEffect(sessionId, cwd) {
        while (true) {
            account = ClaudeCode.account()
            state = if (sessionId.isNotBlank()) ClaudeCode.sessionState(cwd, sessionId)
            else ClaudeCode.latestSession(cwd.ifBlank { null })
            delay(2000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        backgroundColor = colors.background,
        elevation = 5.dp
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            // En-tête : robot + titre éditable + case "fait" (masque la tâche).
            Row(
                Modifier.fillMaxWidth().height(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤖", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        Tasks.update(task.apply { this.title = it })
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = colors.onBackground, fontWeight = FontWeight.SemiBold),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.onBackground)
                )
                Icon(
                    if (done) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(16.dp)
                        .clickable {
                            done = !done
                            Tasks.update(task.apply { this.done = done })
                            Tasks.refresh()
                        },
                    tint = if (done) Color.Green else Color.Red
                )
            }

            Spacer(Modifier.height(6.dp))

            // Ligne d'état du compte + connexion.
            AccountRow(account, colors) { ClaudeCode.login() }

            if (account.connected) {
                Spacer(Modifier.height(6.dp))
                LaunchRow(
                    prompt = prompt,
                    cwd = cwd,
                    colors = colors,
                    onPrompt = { prompt = it; Tasks.update(task.apply { description = it }) },
                    onCwd = { cwd = it; Tasks.update(task.apply { claudeCwd = it }) },
                    onLaunch = {
                        scope.launch {
                            task.description = prompt
                            task.claudeCwd = cwd
                            Tasks.update(task)
                            val started = ClaudeCode.launch(prompt, cwd)
                            // La session apparaît dans la seconde ; on la rattache.
                            repeat(6) {
                                delay(1000)
                                val s = ClaudeCode.latestSession(cwd, after = started)
                                if (s != null) {
                                    sessionId = s.sessionId
                                    Tasks.update(task.apply { claudeSessionId = s.sessionId })
                                    return@launch
                                }
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                SessionView(state, colors)
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: ClaudeCode.Account,
    colors: com.elfefe.common.model.ThemeColors,
    onConnect: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (account.connected && !account.expired) {
            Dot(Color(0xFF3FB950))
            Spacer(Modifier.width(6.dp))
            Text(
                "Connecté" + if (account.plan.isNotBlank()) " · ${account.plan}" else "",
                color = colors.onBackground,
                fontSize = 11.sp
            )
        } else {
            Dot(Color(0xFFCC5555))
            Spacer(Modifier.width(6.dp))
            Text(
                if (account.expired) "Session expirée" else "Compte non connecté",
                color = colors.onBackground,
                fontSize = 11.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Se connecter",
                color = colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onConnect() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LaunchRow(
    prompt: String,
    cwd: String,
    colors: com.elfefe.common.model.ThemeColors,
    onPrompt: (String) -> Unit,
    onCwd: (String) -> Unit,
    onLaunch: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LabeledField("Prompt", prompt, colors, onPrompt, singleLine = false)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { LabeledField("Dossier", cwd, colors, onCwd, singleLine = true) }
            Spacer(Modifier.width(8.dp))
            Text(
                "Lancer",
                color = colors.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary.copy(alpha = if (prompt.isBlank()) 0.4f else 1f))
                    .clickable(enabled = prompt.isNotBlank()) { onLaunch() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    colors: com.elfefe.common.model.ThemeColors,
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
private fun SessionView(state: ClaudeCode.SessionState?, colors: com.elfefe.common.model.ThemeColors) {
    if (state == null) {
        Text("Aucune session suivie pour l'instant.", color = colors.onBackground.copy(alpha = 0.5f), fontSize = 10.sp)
        return
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.running) PulsingDot(Color(0xFF3FB950)) else Dot(colors.onBackground.copy(alpha = 0.4f))
            Spacer(Modifier.width(6.dp))
            Text(
                if (state.running) "En cours" else "Inactif",
                color = if (state.running) Color(0xFF3FB950) else colors.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(state.title, color = colors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        if (state.lastActivity.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(state.lastActivity, color = colors.onBackground.copy(alpha = 0.85f), fontSize = 11.sp)
        }
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
