package com.elfefe.common.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.*
import com.elfefe.common.controller.auth.GoogleAuth
import com.elfefe.common.controller.firebase.authentication.User
import com.elfefe.common.model.HoldKey
import com.elfefe.common.model.TaskFieldOrder
import com.elfefe.common.ui.theme.AppFont
import com.elfefe.common.ui.theme.family
import grayScale
import hexToColor
import maxSaturation
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import toHexString
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class ConfigNavDestination(val text: String) {
    //    EMOTES(Traductions().emotes),
//    CARDS(Traductions().cards),
    GENERAL(Translation().general),
    THEMES(Translation().theme),
}

@Composable
fun Configs(windowInteractions: WindowInteractions) {
    var currentDestination: ConfigNavDestination by remember { mutableStateOf(ConfigNavDestination.GENERAL) }

    Row {
        NavigationBar {
            currentDestination = it
        }
        Spacer(Modifier.width(16.dp))
        Navigator(currentDestination, windowInteractions)
    }
}

@Composable
fun AnimatedNavigation(visible: Boolean, page: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) { page() }
}

@Composable
fun Navigator(destination: ConfigNavDestination, windowInteractions: WindowInteractions) {
    AnimatedNavigation(destination == ConfigNavDestination.GENERAL) { General(windowInteractions) }
    AnimatedNavigation(destination == ConfigNavDestination.THEMES) { Theme(windowInteractions) }
}

@Composable
fun NavigationBar(onNavigate: (ConfigNavDestination) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxHeight()
            .width(128.dp),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFF2C2F33)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(ConfigNavDestination.values()) { destination ->
                TextButton(onClick = {
                    onNavigate(destination)
                }) {
                    Text(
                        text = destination.text,
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun Cards(windowInteractions: WindowInteractions) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        CardsOrder()
    }
}

@Composable
fun CardsOrder() {
    val sortOrders =
        remember { mutableStateOf(Tasks.Configs.configs.taskFieldsOrder.sortedByDescending { it.priority }) }

    val state = rememberReorderableLazyListState(onMove = { from, to ->
        sortOrders.value = sortOrders.value.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    })

    LazyColumn(
        state = state.listState,
        modifier = Modifier
            .reorderable(state)
            .detectReorderAfterLongPress(state)
    ) {
        itemsIndexed(sortOrders.value) { index, taskField ->
            taskField.priority = when {
                taskField.priority > 0 -> index
                taskField.priority < 0 -> -sortOrders.value.size + index - 1
                else -> 0
            }
            ReorderableItem(state, key = taskField) {
                CardsOrderCondition(
                    modifier = Modifier
                        .width(256.dp)
                        .height(if (state.draggingItemIndex == index) 48.dp else 42.dp),
                    elevation = if (state.draggingItemIndex == index) 8.dp else 2.dp,
                    taskField = taskField
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CardsOrderCondition(modifier: Modifier, elevation: Dp, taskField: TaskFieldOrder) {
    Card(modifier = modifier, elevation = elevation) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var orderActive by remember { mutableStateOf(taskField.active) }
            var orderRotation by remember { mutableStateOf(if (taskField.priority > 0) 180f else 0f) }
            val orderRotationAnimation by animateFloatAsState(if (orderRotation == 0f) 180f else 0f)

            fun updateConfigs() {
                Tasks.Configs.configs.taskFieldsOrder.filter { it.name == taskField.name }.onEach {
                    it.active = taskField.active
                    it.priority = taskField.priority
                }

                Tasks.refresh()
                Tasks.Configs.update()
            }

            Checkbox(checked = orderActive, onCheckedChange = {
                orderActive = it
                taskField.active = it

                updateConfigs()
            })

            Text(
                text = taskField.name,
                fontWeight = FontWeight.Thin,
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            IconButton({
                orderRotation = if (orderRotation == 0f) 180f else 0f

                if (taskField.priority != 0)
                    taskField.priority *= -1

                updateConfigs()
            }) {
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint = Color.Black,
                    modifier = Modifier.rotate(orderRotationAnimation)
                )
            }
        }
    }
}

@Composable
fun Theme(windowInteractions: WindowInteractions) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            FontConfig()
            Spacer(Modifier.height(16.dp))
        }
        themePartConfig(Translation().toolbarBackground, Tasks.Configs.configs.themeColors.primary) {
            Tasks.Configs.configs.updateThemeColors(primary = it)
            Tasks.Configs.update()
        }
        themePartConfig(Translation().toolbarIcons, Tasks.Configs.configs.themeColors.onPrimary) {
            Tasks.Configs.configs.updateThemeColors(onPrimary = it)
            Tasks.Configs.update()
        }
        /*themePartConfig("Secondary", Tasks.Configs.configs.themeColors.secondary) {
            Tasks.Configs.configs.updateThemeColors(secondary = it)
            Tasks.Configs.update()
        }
        themePartConfig("On secondary", Tasks.Configs.configs.themeColors.onSecondary) {
            Tasks.Configs.configs.updateThemeColors(onSecondary = it)
            Tasks.Configs.update()
        }*/
        themePartConfig(Translation().tasksBackground, Tasks.Configs.configs.themeColors.background) {
            Tasks.Configs.configs.updateThemeColors(background = it)
            Tasks.Configs.update()
        }
        themePartConfig(Translation().tasksContent, Tasks.Configs.configs.themeColors.onBackground) {
            Tasks.Configs.configs.updateThemeColors(onBackground = it)
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = Translation().resetTheme,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Tasks.Configs.configs.themeColors.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Tasks.Configs.configs.themeColors.primary)
                    .clickable { Tasks.Configs.configs.resetThemeColors() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Compte Google.
 *
 * Il y avait ici deux champs — adresse et mot de passe — envoyés à Firebase
 * avec une clé d'API qui a depuis été purgée du dépôt : le formulaire ne menait
 * donc plus nulle part. La connexion passe maintenant par le navigateur, où la
 * session Google est déjà ouverte : un accord donné une fois, et l'application
 * se reconnecte seule ensuite.
 */
@Composable
fun AccountConfig() {
    val colors = Tasks.Configs.configs.themeColors
    val state = GoogleAuth.state

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = Translation().accountLabel,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = colors.onBackground
        )
        Spacer(Modifier.height(8.dp))

        when (state) {
            is GoogleAuth.State.SignedIn -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.name.ifBlank { state.email.ifBlank { Translation().accountConnected } },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    if (state.email.isNotBlank() && state.name.isNotBlank())
                        Text(state.email, fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.7f))
                }
                Text(
                    text = Translation().signOutLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.onBackground.copy(alpha = 0.08f))
                        .clickable { GoogleAuth.signOut() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            is GoogleAuth.State.Connecting -> Text(
                text = Translation().accountConnecting,
                fontSize = 13.sp,
                color = colors.onBackground.copy(alpha = 0.7f)
            )

            else -> Column {
                if (state is GoogleAuth.State.Failed) {
                    Text(state.reason, fontSize = 12.sp, color = Color(0xFFCC5555))
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = Translation().signInLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primary)
                        .clickable { GoogleAuth.signIn() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Translation().signInHint,
                    fontSize = 12.sp,
                    color = colors.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Choix de la touche qui, maintenue, empeche la pile de se deployer a
 * l'approche et libere la poignee.
 */
@Composable
fun HoldKeyConfig() {
    val colors = Tasks.Configs.configs.themeColors
    val current = Tasks.Configs.configs.holdKey

    Column {
        Text(
            text = Translation().holdKeyLabel,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = colors.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = Translation().holdKeyHint,
            fontSize = 12.sp,
            color = colors.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HoldKey.values().forEach { key ->
                val selected = key == current
                Text(
                    text = key.label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) colors.onPrimary else colors.onBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) colors.primary
                            else colors.onBackground.copy(alpha = 0.08f)
                        )
                        .clickable { Tasks.Configs.configs.updateHoldKey(key) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun General(windowInteractions: WindowInteractions) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        var startOnBoot by remember { mutableStateOf(startupAppFile.exists()) }

        Card(
            modifier = Modifier
                .fillMaxSize(),
            elevation = 4.dp,
            backgroundColor = Color.White
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { HoldKeyConfig() }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = startOnBoot, onCheckedChange = {

                            if (!startupAppFile.exists()) {
                                try {
                                    windowInteractions.popup.value = Popup.show(createShortcutWithAdminRights())
                                    startOnBoot = true
                                } catch (e: Exception) {
                                    windowInteractions.popup.value = Popup.show(e.message ?: "Error while creating link")
                                }
                            } else {
                                try {
                                    windowInteractions.popup.value = Popup.show(deleteShortcutWithAdminRights())
                                    startOnBoot = false
                                } catch (e: Exception) {
                                    windowInteractions.popup.value = Popup.show(e.message ?: "Error while deleting link")
                                }
                            }
                        })

                        Spacer(Modifier.width(16.dp))

                        Text(
                            text = Translation().startupLabel,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = Tasks.Configs.configs.themeColors.onBackground
                        )
                    }
                }

                item { AccountConfig() }
            }
        }
    }
}

/**
 * Choix de la police.
 *
 * Chaque option est écrite dans sa propre police et suivie d'un aperçu au corps
 * et sur le fond réellement utilisés par les tâches : une police se juge à la
 * taille où on la lira, en clair sur sombre, pas dans une liste de noms tous
 * composés pareil.
 */
@Composable
fun FontConfig() {
    val selected = Tasks.Configs.configs.font
    val colors = Tasks.Configs.configs.themeColors

    Card(shape = RoundedCornerShape(8.dp), backgroundColor = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = Translation().fontLabel,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Translation().fontHint,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(12.dp))

            AppFont.values().forEach { font ->
                val chosen = font == selected
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (chosen) colors.primary.copy(alpha = 1f) else Color(0xFFF2F2F2))
                        .clickable {
                            Tasks.Configs.configs.updateFont(font)
                            Tasks.Configs.update()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = font.label,
                            fontFamily = font.family(),
                            fontSize = 15.sp,
                            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (chosen) colors.onPrimary else Color.DarkGray
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = font.description,
                            fontSize = 11.sp,
                            color = if (chosen) colors.onPrimary.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Même corps et même fond que les cartes : c'est là que se
                    // joue la lisibilité, pas dans un échantillon en grand.
                    Text(
                        text = Translation().fontSample,
                        fontFamily = font.family(),
                        fontSize = 11.sp,
                        color = colors.onBackground,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.background)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.themePartConfig(label: String, defaultColor: Color, onColorChange: (Color) -> Unit) {
    stickyHeader {
        Card(
            shape = RoundedCornerShape(8.dp),
            backgroundColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
                Spacer(Modifier.height(8.dp))
                ThemeColor(defaultColor, onColorChange)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ThemeColor(default: Color, onColorChange: (Color) -> Unit) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .width(256.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var currentColor by remember { mutableStateOf(default) }

        var colorCursorPosition by remember { mutableStateOf(0f) }
        var darknessCursorPosition by remember { mutableStateOf((default.red + default.blue + default.green) / 3 * 180) }
        var alphaCursorPosition by remember { mutableStateOf(default.alpha) }
        var saturated by remember { mutableStateOf(default.red != default.blue && default.blue != default.green) }

        /**
         * Vrai dès que l'utilisateur a touché un curseur.
         *
         * `currentColor` est reconstruit à partir de la position des curseurs,
         * eux-mêmes déduits de la couleur par une approximation : la couleur
         * ainsi retrouvée n'est jamais tout à fait celle d'origine. Comme le
         * changement était émis depuis le dessin de l'aperçu, ouvrir l'onglet
         * Thème suffisait à repeindre le thème d'une teinte voisine — ce qui
         * n'était visible que depuis que les couleurs s'enregistrent vraiment.
         */
        var touched by remember { mutableStateOf(false) }

        LaunchedEffect(currentColor) { if (touched) onColorChange(currentColor) }

        Column(
            modifier = Modifier
                .width(180.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val colorGradient = listOf(
                Color(1f, 0f, 0f),
                Color(1f, 1f, 0f),
                Color(0f, 1f, 0f),
                Color(0f, 1f, 1f),
                Color(0f, 0f, 1f),
                Color(1f, 0f, 1f),
                Color(1f, 0f, 0f),
            )

            Text(
                text = Translation().color,
                fontWeight = FontWeight.Thin,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            touched = true
                            colorCursorPosition = change.position.x
                        }
                    }
            ) {
                val colorAlphaGradient =
                    listOf(
                        Color(1f, 0f, 0f, alphaCursorPosition),
                        Color(1f, 1f, 0f, alphaCursorPosition),
                        Color(0f, 1f, 0f, alphaCursorPosition),
                        Color(0f, 1f, 1f, alphaCursorPosition),
                        Color(0f, 0f, 1f, alphaCursorPosition),
                        Color(1f, 0f, 1f, alphaCursorPosition),
                        Color(1f, 0f, 0f, alphaCursorPosition),
                    )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colorGradient,
                        tileMode = TileMode.Clamp
                    ),
                    size = size,
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color.LightGray,
                    topLeft = Offset(max(0f, min(size.width, colorCursorPosition)), 0f),
                    size = Size(2f, size.height),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                val colorIndexNormalized = (colorCursorPosition / size.width).let { if (it.isNaN()) 0f else it }
                val firstColorIndex = max(
                    0, min(
                        colorAlphaGradient.size - 2,
                        floor((colorIndexNormalized) * (colorAlphaGradient.size - 1)).toInt()
                    )
                )

                val fractionStep = 1f / (colorAlphaGradient.size - 1)
                val minFraction = ((firstColorIndex + 1) / (colorAlphaGradient.size - 1f)) - fractionStep
                val colorFraction =
                    max(0f, min(1f, (colorIndexNormalized - minFraction) * (colorAlphaGradient.size - 1f)))

                if (saturated) currentColor =
                    if (colorAlphaGradient.lastIndex == firstColorIndex) colorAlphaGradient[firstColorIndex]
                    else lerp(
                        colorAlphaGradient[firstColorIndex],
                        colorAlphaGradient[firstColorIndex + 1],
                        colorFraction
                    )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = Translation().brightness,
                fontWeight = FontWeight.Thin,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            touched = true
                            darknessCursorPosition = change.position.x
                        }
                    }
            ) {
                val colorGradient = listOf(Color.Black, if (saturated) currentColor else Color.Gray, Color.White)
                val colorAlphaGradient = listOf(
                    Color(0f, 0f, 0f, alphaCursorPosition),
                    if (saturated) currentColor.maxSaturation() else Color.Gray,
                    Color(1f, 1f, 1f, alphaCursorPosition)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colorGradient,
                        tileMode = TileMode.Clamp
                    ),
                    cornerRadius = CornerRadius(4f, 4f),
                    size = size
                )
                drawRoundRect(
                    color = currentColor.grayScale().run {
                        if ((red + green + blue) / 3 > .5f ) Color.DarkGray else Color.LightGray
                    },
                    topLeft = Offset(max(0f, min(size.width, darknessCursorPosition)), 0f),
                    size = Size(2f, size.height),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                val indexNormalized = (darknessCursorPosition / size.width).let {
                    if (it.isNaN()) 0f else min(1f, max(0f, it))
                }
                val colorIndex =
                    min(floor(indexNormalized * (colorAlphaGradient.size - 1)).toInt(), colorAlphaGradient.size - 2)

                val fractionStep = 1f / (colorAlphaGradient.size - 1)
                val minFraction = ((colorIndex + 1) / (colorAlphaGradient.size - 1f)) - fractionStep
                val colorLerpFraction =
                    max(0f, min(1f, (indexNormalized - minFraction) * (colorAlphaGradient.size - 1f)))

                currentColor =
                    lerp(colorAlphaGradient[colorIndex], colorAlphaGradient[colorIndex + 1], colorLerpFraction)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = Translation().opacity,
                fontWeight = FontWeight.Thin,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Slider(
                    value = alphaCursorPosition,
                    onValueChange = { touched = true; alphaCursorPosition = it },
                    modifier = Modifier
                        .fillMaxWidth(.8f)
                        .fillMaxHeight()
                )

                Spacer(Modifier.width(4.dp))

                Checkbox(saturated, { touched = true; saturated = !saturated })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Canvas(
                modifier = Modifier
                    .size(64.dp)
            ) {
                // L'aperçu se contente de montrer la couleur : la publier d'ici
                // revenait à écrire le thème à chaque frame, y compris au tout
                // premier dessin. C'est le `LaunchedEffect` plus haut qui la
                // remonte, une fois l'utilisateur passé par un curseur.
                drawRoundRect(
                    color = currentColor,
                    cornerRadius = CornerRadius(8f, 8f),
                    size = size
                )
            }

            Spacer(Modifier.height(8.dp))

            BasicTextField(
                value = currentColor.toHexString(),
                onValueChange = {
                    println(it)
                    var colorText = it
                    if (colorText.startsWith("#"))
                        colorText = colorText.substring(1)
                    if (colorText.length != 8) return@BasicTextField

                    try {
                        currentColor = colorText.hexToColor()
                        onColorChange(currentColor)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                readOnly = false,
                modifier = Modifier
                    .padding(0.dp)
                    .width(64.dp)
                    .align(Alignment.CenterHorizontally),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.Black,
                    fontSize = 12.sp
                ),
                singleLine = true,
                visualTransformation = {
                    TransformedText(
                        text = AnnotatedString("#${it.text}"),
                        offsetMapping = object : OffsetMapping {
                            override fun originalToTransformed(offset: Int): Int {
                                return offset + 1
                            }

                            override fun transformedToOriginal(offset: Int): Int {
                                return when {
                                    offset == 1 -> 0
                                    offset > it.text.length -> it.text.length
                                    else -> offset
                                }
                            }

                        }
                    )
                }
            )
        }
    }
}