package com.elfefe.common.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.elfefe.common.controller.CrashWindow
import com.elfefe.common.controller.Tasks
import com.elfefe.common.ui.theme.TasksTheme

/** Largeur de l'infobulle d'aide, en dp. */
const val HELP_WINDOW_WIDTH = 400f

/**
 * Mode d'emploi affiche au clic sur le point d'interrogation de la barre de
 * navigation. Le texte est du markdown, rendu par le meme moteur que les
 * reponses de Claude : titres, listes et emphases sans mise en page a la main.
 */
private fun helpText(holdKey: String): String = """
# Utiliser TasksWidget

## La pile

- La pile vit sur un bord de l'écran. **Approchez la souris de la poignée** pour
  la déployer, **éloignez-vous** pour la replier.
- **Glissez la barre de navigation** pour déplacer la pile ; elle s'aimante au
  bord le plus proche.
- La **molette** fait défiler les cartes.

## Déplacer la poignée

- Maintenez **$holdKey** : la pile ne se déploie plus à l'approche, et la
  poignée s'épaissit — signe qu'elle est saisissable.
- **Glissez-la** pour la monter ou la descendre ; amenez-la de l'autre côté de
  l'écran pour faire basculer toute la pile. Sa place est retenue.

## La barre de navigation

- **Notes** : afficher ou masquer les descriptions des tâches.
- **Coche** : afficher aussi les tâches terminées.
- **+** : ajouter une tâche, ou ouvrir une session Claude.
- **Loupe** : rechercher dans les titres, dates et descriptions.
- **Roue dentée** : réglages, dont les couleurs et la touche ci-dessus.
- **Épingle** : maintient la pile déployée quoi qu'il arrive — souris partie,
  clic ailleurs. Elle prime sur tout le reste, et son état est retenu.

## Les tâches

- La date à gauche est l'échéance au format `JJ/MM` : **rouge** aujourd'hui,
  **jaune** dépassée.
- Le **crayon** ouvre la barre markdown : titres, gras, italique, barré, listes,
  code, surlignage.
- La **croix** marque la tâche comme faite ; elle disparaît de la liste tant que
  l'affichage des tâches terminées est désactivé.

## Les sessions Claude Code

- Chaque session ouverte sur le PC apparaît en tête, avec son nom.
- La **pastille** dit l'état : disque plein quand la session travaille, anneau
  quand elle est prête.
- **Survolez la pastille** pour un aperçu des dernières réponses, **cliquez-la**
  pour ouvrir la conversation entière.
- Le **crayon** écrit un message markdown et l'envoie à la vraie session du
  terminal.
""".trimIndent()

@Composable
fun HelpWindow(xDp: Float, yDp: Float, onClose: () -> Unit) {
    val colors = Tasks.Configs.configs.themeColors
    val holdKey = Tasks.Configs.configs.holdKey.label

    val state = rememberWindowState(
        position = WindowPosition.Absolute(xDp.dp, yDp.dp),
        size = DpSize(HELP_WINDOW_WIDTH.dp, 520.dp)
    )
    LaunchedEffect(xDp, yDp) { state.position = WindowPosition.Absolute(xDp.dp, yDp.dp) }

    CrashWindow(
        onCloseRequest = onClose,
        state = state,
        visible = true,
        title = "Tasks - aide",
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
                Column(Modifier.fillMaxSize().padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HelpOutline, null,
                            tint = colors.onBackground, modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Aide",
                            color = colors.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.Close, "Fermer",
                            tint = colors.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp).clip(CircleShape).clickable { onClose() }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        MarkdownText(
                            markdown = helpText(holdKey),
                            colors = colors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
