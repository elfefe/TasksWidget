package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.StateFilter
import com.elfefe.common.controller.Tasks
import com.elfefe.common.model.Task
import com.elfefe.common.model.TaskState
import com.elfefe.common.model.ThemeColors
import kotlinx.coroutines.CoroutineScope
import java.awt.Desktop
import java.net.URI
import java.util.Locale


@Composable
fun ColumnScope.Toolbar(
    scope: CoroutineScope,
    windowInteractions: WindowInteractions,
    toolbarInteractions: ToolbarInteractions
) {

    var showConfigs by remember { mutableStateOf(false) }

    // Menu du filtre d'états : ouvert et refermé par le même bouton de la barre.
    var showStates by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(true) }

    var expanded by remember { mutableStateOf(true) }
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f)

    var showSearch by remember { mutableStateOf(false) }

    var searching by remember { mutableStateOf("") }

    // Menu du « + » : choisir entre une tâche normale et une session Claude.
    var showAddMenu by remember { mutableStateOf(false) }
    var showClaudeLaunch by remember { mutableStateOf(false) }

    val colors = Tasks.Configs.configs.themeColors

    windowInteractions.expand.addOnChange("Toolbar") { expanded = it }

    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        windowInteractions.moveWindow.isActive = true
                        windowInteractions.moveWindow.origin = it.x
                    },
                    onDragEnd = {
                        windowInteractions.moveWindow.isActive = false
                        windowInteractions.moveWindow.origin = 0f
                    },
                    onDragCancel = {
                        windowInteractions.moveWindow.isActive = false
                        windowInteractions.moveWindow.origin = 0f
                    }
                ) { change, dragAmount ->
                    windowInteractions.moveWindow.value = change.position.x
                }
            }
            .fillMaxWidth()
            .background(
                color = Tasks.Configs.configs.themeColors.primary,
                shape = RoundedCornerShape(5.dp)
            )
    ) {
        val composables = listOf<@Composable () -> Unit>(
            {
                Icon(
                    painterResource(if (!showDescription) "baseline_notes_24.svg" else "short_text_24px.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showDescription = !showDescription
                            toolbarInteractions.showDescription(showDescription)
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                // Filtre d'états : l'entonnoir se remplit dès que la sélection
                // s'écarte de celle d'origine — sans quoi rien ne dirait qu'une
                // partie des cartes est tue.
                Icon(
                    if (StateFilter.isDefault()) Icons.Default.FilterList else Icons.Default.FilterAlt,
                    contentDescription = "Filtrer par état",
                    modifier = Modifier
                        .clickable { showStates = !showStates }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary.run {
                        if (showStates) copy(alpha = .6f) else this
                    }
                )
            },
            {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showAddMenu = !showAddMenu
                            if (!showAddMenu) showClaudeLaunch = false
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            showSearch = !showSearch
                        }
                        .padding(3.dp),
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            /*Icon(
                painterResource("login.svg"),
                contentDescription = null,
                modifier = Modifier
                    .padding(3.dp)
                    .clickable {
                        FirestoreApi.instance.connectTasks(
                            User("felion33@gmail.com", "Félix", "", mutableListOf())
                        ) {
                            println(it)
                        }
                    OAuthApi(scope).apply {
                        auth(
                            "1086878445333-tgnhihe3rkaigfqs39umarbfsptb1lr5.apps.googleusercontent.com",
                            "***SECRET-PURGE-2026-07-27***"
                        ) { jwToken, payload ->
                            println(jwToken)
                            println(payload)
                            val credentials = GoogleCredentials
                                .create(AccessToken(jwToken.accessToken, Date(Date().time + jwToken.expiresIn.toLong())))
                                .createScoped(
                                    "https://www.googleapis.com/auth/cloud-platform"
                                )
                        }
                    }
                    },
                tint = Color.White
            )*/
            {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable {
                            showConfigs = !(windowInteractions.showConfigs.value ?: false)
                            windowInteractions.showConfigs.value = showConfigs
                        },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                // Épingle : tant qu'elle est active, la pile reste déployée
                // quoi qu'il arrive. Pleine quand elle tient, contour sinon.
                val pinned = toolbarInteractions.pinned()
                Icon(
                    if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (pinned) "Détacher" else "Épingler",
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable { toolbarInteractions.togglePinned() },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = "Aide",
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable { toolbarInteractions.toggleHelp() },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            },
            {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(3.dp)
                        .clickable {
                            fun openInBrowser(uri: URI) {
                                val osName by lazy(LazyThreadSafetyMode.NONE) { System.getProperty("os.name").lowercase(
                                    Locale.getDefault()) }
                                val desktop = Desktop.getDesktop()
                                when {
                                    Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> desktop.browse(uri)
                                    "mac" in osName -> Runtime.getRuntime().exec("open $uri")
                                    "nix" in osName || "nux" in osName -> Runtime.getRuntime().exec("xdg-open $uri")
                                    else -> throw RuntimeException("cannot open $uri")
                                }
                            }

                            openInBrowser(URI("https://domain.tld/page"))
                        },
                    tint = Tasks.Configs.configs.themeColors.onPrimary
                )
            })
        Row(
            modifier = Modifier
                .padding(8.dp, 0.dp)
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = if (windowInteractions.moveWindow.isRight.value) Arrangement.End else Arrangement.Start,
        ) {
            val parameters = if (windowInteractions.moveWindow.isRight.value) composables else composables.reversed()
            parameters.forEach { it() }
        }

//            Row(horizontalArrangement = Arrangement.End) {
//                Icon(
//                    Icons.Default.ArrowDropDown,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .clickable {
//                            windowInteractions.expand.value = !expanded
//                        }
//                        .padding(3.dp)
//                        .rotate(expandRotation),
//                    tint = Tasks.Configs.configs.themeColors.onPrimary
//                )
//                Icon(
//                    Icons.Default.ExitToApp,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .clickable {
//                            windowInteractions.expand.value = false
//                        }
//                        .padding(3.dp),
//                    tint = Tasks.Configs.configs.themeColors.onPrimary
//                )
//            }
        AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Tasks.Configs.configs.themeColors.primary.run {
                            Color(red, green, blue, .6f)
                        },
                        shape = RoundedCornerShape(5.dp)
                    )
            ) {
                BasicTextField(
                    value = searching,
                    onValueChange = {
                        searching = it
                        // Les filtres se cumulent : celui des états dit quelles
                        // cartes sont montrées, celui-ci ce qu'on y cherche.
                        Tasks.filter("searching") { task ->
                            task.title.contains(searching, true) ||
                                    task.deadline.contains(searching, true) ||
                                    task.description.contains(searching, true)
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = Tasks.Configs.configs.themeColors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 5.dp),
                    cursorBrush = SolidColor(Tasks.Configs.configs.themeColors.onPrimary)
                )
            }
        }

        AnimatedVisibility(visible = showStates, enter = expandVertically(), exit = shrinkVertically()) {
            StateFilterMenu(colors)
        }

        AnimatedVisibility(visible = showAddMenu, enter = expandVertically(), exit = shrinkVertically()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp, 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary.copy(alpha = 0.35f))
                        .clickable {
                            Tasks.update(Task())
                            showAddMenu = false
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = colors.onPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tâche", color = colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary.copy(alpha = 0.35f))
                        .clickable { showClaudeLaunch = !showClaudeLaunch }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = colors.onPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Session Claude", color = colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = showAddMenu && showClaudeLaunch,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ClaudeLaunchPanel(colors) {
                showClaudeLaunch = false
                showAddMenu = false
            }
        }
    }
}

/**
 * Menu déroulant du filtre d'états : une ligne par état, cochée tant que cet
 * état est montré dans la pile. Plusieurs états peuvent tenir ensemble, et
 * chaque clic prend effet aussitôt — le bouton de la barre ne fait qu'ouvrir et
 * refermer le menu.
 */
@Composable
private fun StateFilterMenu(colors: ThemeColors) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        TaskState.values().forEach { state ->
            // Les sessions Claude d'abord, les tâches ensuite : un écart les
            // sépare, les deux familles ne se filtrent pas pour les mêmes
            // raisons.
            if (state == TaskState.TODO) Spacer(Modifier.height(5.dp))

            val checked = StateFilter.isSelected(state)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary.copy(alpha = if (checked) 0.35f else 0.12f))
                    .clickable { StateFilter.toggle(state) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = colors.onPrimary.run { if (checked) this else copy(alpha = .55f) },
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    state.label,
                    color = colors.onPrimary.run { if (checked) this else copy(alpha = .55f) },
                    fontSize = 11.sp,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}