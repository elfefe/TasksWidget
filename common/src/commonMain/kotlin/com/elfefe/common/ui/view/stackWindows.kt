package com.elfefe.common.ui.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.elfefe.common.controller.CrashWindow
import com.elfefe.common.controller.Tasks
import com.elfefe.common.controller.Updater
import com.elfefe.common.controller.isRemoteNewer
import com.elfefe.common.controller.log
import com.elfefe.common.controller.update
import com.elfefe.common.model.Task
import com.elfefe.common.model.github.GithubLatestRelease
import com.elfefe.common.ui.theme.TasksTheme
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs

/**
 * Refonte multi-fenêtres : au lieu d'une grande fenêtre unique, chaque tâche et
 * la barre de navigation sont des fenêtres transparentes non décorées,
 * dimensionnées exactement à leur contenu, empilées à la même abscisse. Un
 * système de scroll custom déplace la pile, un système de repli la fait
 * coulisser hors écran vers une poignée (comportement conservé de l'ancienne
 * version).
 */

/**
 * Pas de défilement par cran de molette, en dp. On avance d'un pas fixe par
 * événement (sens de [scrollDelta] seulement) : l'amplitude brute de la molette
 * varie trop d'un pilote à l'autre pour être fiable.
 */
private const val SCROLL_STEP = 100f

/** État partagé par toutes les fenêtres de la pile. */
class StackController(val workArea: Rectangle) {
    val stackWidth: Dp = WINDOW_MAX_WIDTH

    /** Bord d'ancrage : true = droite de l'écran. */
    var isRight by mutableStateOf(true)

    /** Abscisse (dp) du bord gauche de la pile, hors animation de repli. */
    var baseX by mutableStateOf((workArea.x + workArea.width - stackWidth.value))

    /** Ordonnée (dp) du haut de la barre de navigation. */
    val baseY: Float get() = workArea.y.toFloat()

    /** Hauteur mesurée de la barre de navigation (dp). */
    var navHeight by mutableStateOf(0f)

    /** Hauteur mesurée de chaque tâche (dp), indexée par [Task.created]. */
    val heights = mutableStateMapOf<Long, Float>()

    /** Décalage vertical du scroll (dp), >= 0. */
    var scrollOffset by mutableStateOf(0f)

    /** Pile déployée ou repliée sur le bord. */
    var expanded by mutableStateOf(true)

    /** Un glisser de déplacement est en cours : on suspend le repli auto. */
    var moveActive by mutableStateOf(false)

    /** Description dépliée sur les cartes (piloté par la toolbar). */
    var showDescription by mutableStateOf(true)

    /** Liste courante des tâches filtrées/triées. */
    var tasks by mutableStateOf(listOf<Task>())

    /**
     * Instant du dernier changement de liste (ajout / suppression / « fait »).
     * Cocher « fait » retire la tâche : l'aire des tâches rétrécit sous le
     * curseur, qui se retrouve dehors. Sans grâce, le repli auto se déclenche et
     * la pile disparaît — ce que l'utilisateur ne veut pas. On suspend donc le
     * repli un court instant après chaque changement.
     */
    var lastChange by mutableStateOf(0L)

    /** Fenêtre AWT de la barre de navigation, pour la ramener au premier plan. */
    var navWindow: Window? = null

    val viewportTop: Float get() = baseY + navHeight
    val viewportBottom: Float get() = (workArea.y + workArea.height).toFloat()
    val viewportHeight: Float get() = (viewportBottom - viewportTop).coerceAtLeast(0f)

    fun totalContent(): Float {
        var sum = 0f
        tasks.forEach { sum += heights[it.created] ?: 0f }
        return sum
    }

    fun maxScroll(): Float = (totalContent() - viewportHeight).coerceAtLeast(0f)

    fun scrollBy(deltaDp: Float) {
        scrollOffset = (scrollOffset + deltaDp).coerceIn(0f, maxScroll())
    }

    /** Somme des hauteurs des tâches précédant [index]. */
    fun cumulativeBefore(index: Int): Float {
        var sum = 0f
        for (i in 0 until index) sum += heights[tasks[i].created] ?: 0f
        return sum
    }

    fun setHeight(created: Long, dp: Float) {
        val current = heights[created]
        if (current == null || abs(current - dp) > 0.5f) heights[created] = dp
    }

    // Géométrie de la poignée de repli (partagée entre le sondage et la fenêtre
    // poignée pour que la zone qui déploie corresponde exactement à ce qui est
    // dessiné au bord).
    val handleWidth = 24f
    val handleHeight = 64f
    fun handleLeft(): Float =
        if (isRight) workArea.x + workArea.width - handleWidth else workArea.x.toFloat()

    /** Aire réellement occupée par la pile déployée (nav + tâches). */
    fun stackBottom(): Float = (viewportTop + totalContent()).coerceAtMost(viewportBottom)

    // Poignée centrée verticalement sur l'aire des tâches : ainsi, en déployant
    // depuis la poignée, la souris se trouve d'emblée dans la zone des tâches et
    // ne déclenche pas un repli immédiat.
    fun handleTop(): Float = ((baseY + stackBottom()) / 2f - handleHeight / 2f).coerceAtLeast(baseY)
}

@Composable
fun ApplicationScope.TaskStack(windowInteractions: WindowInteractions) {
    val workArea = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds }
    val controller = remember { StackController(workArea) }

    // Alimentation en tâches.
    LaunchedEffect(Unit) {
        Tasks.onUpdate = {
            controller.tasks = it
            controller.lastChange = System.currentTimeMillis()
        }
        Tasks.filter("show done") { !it.done }
        Tasks.refresh()
    }

    // Déplacement de la pile : la toolbar publie sa position de glisser dans
    // windowInteractions.moveWindow ; on la traduit en abscisse absolue.
    DisposableEffect(Unit) {
        windowInteractions.moveWindow.addOnChange("Stack") {
            val globalX = MouseInfo.getPointerInfo().location.x
            controller.baseX = globalX - windowInteractions.moveWindow.origin
            controller.moveActive = true
            controller.isRight =
                controller.baseX + controller.stackWidth.value / 2 > workArea.x + workArea.width / 2
            windowInteractions.moveWindow.isRight.value = controller.isRight
        }
        onDispose { }
    }

    // Repli auto + fin de glisser : sondage de la position souris.
    DisposableEffect(controller) {
        var wasDragging = false
        val timer = fixedRateTimer("stackProximity", initialDelay = 200, period = 50) {
          runCatching {
            val dragging = windowInteractions.moveWindow.isActive
            if (wasDragging && !dragging) {
                // Fin de glisser : on aimante sur le bord le plus proche.
                controller.isRight =
                    controller.baseX + controller.stackWidth.value / 2 > workArea.x + workArea.width / 2
                controller.baseX =
                    if (controller.isRight) workArea.x + workArea.width - controller.stackWidth.value
                    else workArea.x.toFloat()
                windowInteractions.moveWindow.isRight.value = controller.isRight
                controller.moveActive = false
            }
            wasDragging = dragging

            if (!dragging) {
                val mouse = MouseInfo.getPointerInfo().location
                fun inside(l: Float, t: Float, r: Float, b: Float, thr: Int): Boolean =
                    mouse.x in (l - thr).toInt()..(r + thr).toInt() &&
                            mouse.y in (t - thr).toInt()..(b + thr).toInt()

                if (controller.expanded) {
                    // Déployée : on replie dès que la souris quitte l'aire des
                    // tâches (nav + cartes). Mais tant que toutes les fenêtres
                    // n'ont pas mesuré leur hauteur au moins une fois, on reste
                    // déployé : une fenêtre masquée ne se compose pas, donc la
                    // replier trop tôt laisserait les hauteurs à 0 pour toujours
                    // (poignée mal placée, aire des tâches vide).
                    val measured = controller.navHeight > 0f &&
                            controller.tasks.all { controller.heights.containsKey(it.created) }
                    // Grâce après un changement de liste : cocher « fait » fait
                    // rétrécir l'aire sous le curseur ; on ne replie pas dans la
                    // foulée, le temps que l'utilisateur bouge.
                    val settled = System.currentTimeMillis() - controller.lastChange > 900
                    if (measured && settled) {
                        val left = controller.baseX
                        val right = controller.baseX + controller.stackWidth.value
                        if (!inside(left, controller.baseY, right, controller.stackBottom(), 16))
                            controller.expanded = false
                    }
                } else {
                    // Repliée : seule la poignée au bord déploie la pile. Petite
                    // tolérance pour qu'elle reste facile à viser sans pour
                    // autant redevenir une bande pleine hauteur.
                    val left = controller.handleLeft()
                    val top = controller.handleTop()
                    if (inside(left, top, left + controller.handleWidth, top + controller.handleHeight, 16))
                        controller.expanded = true
                }
            }
          }
        }
        onDispose { timer.cancel() }
    }

    // Animation de repli : 0 = déployé, 1 = replié hors écran.
    val slide = remember { Animatable(0f) }
    LaunchedEffect(controller.expanded) {
        slide.animateTo(if (controller.expanded) 0f else 1f, tween(400, easing = EaseInOutCubic))
    }
    val slidePx = slide.value * controller.stackWidth.value * (if (controller.isRight) 1f else -1f)
    val fullyCollapsed = slide.value >= 0.999f

    // La barre de nav doit rester au-dessus des tâches qui coulissent sous elle.
    LaunchedEffect(controller.scrollOffset) {
        controller.navWindow?.toFront()
    }

    // Barre de navigation.
    NavWindow(
        controller = controller,
        windowInteractions = windowInteractions,
        xDp = controller.baseX + slidePx,
        yDp = controller.baseY,
        visible = !fullyCollapsed
    )

    // Fenêtres de tâches.
    controller.tasks.forEachIndexed { index, task ->
        key(task.created) {
            val top = controller.viewportTop + controller.cumulativeBefore(index) - controller.scrollOffset
            val height = controller.heights[task.created] ?: 0f
            val visible = !fullyCollapsed &&
                    (top + height > controller.viewportTop) &&
                    (top < controller.viewportBottom)
            TaskWindow(
                controller = controller,
                windowInteractions = windowInteractions,
                task = task,
                xDp = controller.baseX + slidePx,
                yDp = top,
                visible = visible
            )
        }
    }

    // Poignée de repli sur le bord.
    StackHandle(controller, visible = fullyCollapsed)
}

/**
 * Fenêtre transparente non décorée dimensionnée exactement à son contenu. La
 * mesure se fait à hauteur infinie (découplée de la taille courante de la
 * fenêtre), sinon la fenêtre ne pourrait jamais grandir au-delà de sa taille
 * initiale.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SizedWindow(
    controller: StackController,
    xDp: Float,
    yDp: Float,
    visible: Boolean,
    focusable: Boolean,
    onHeight: (Float) -> Unit,
    onWindow: (Window) -> Unit = {},
    content: @Composable () -> Unit
) {
    // On démarre petit : wrapContentHeight(unbounded) laisse le contenu se
    // mesurer au-delà de la taille de la fenêtre, puis on ajuste la fenêtre à
    // la hauteur réelle depuis onGloballyPositioned (callback post-layout, donc
    // écriture d'état autorisée — l'écrire pendant la passe de mesure planterait
    // Compose à chaque frame).
    var heightDp by remember { mutableStateOf(48f) }

    val state = rememberWindowState(
        position = WindowPosition.Absolute(xDp.dp, yDp.dp),
        size = DpSize(controller.stackWidth, heightDp.dp)
    )

    LaunchedEffect(xDp, yDp) {
        state.position = WindowPosition.Absolute(xDp.dp, yDp.dp)
    }
    LaunchedEffect(heightDp) {
        state.size = DpSize(controller.stackWidth, heightDp.dp)
    }

    CrashWindow(
        onCloseRequest = { },
        state = state,
        visible = visible,
        title = "Tasks",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = focusable,
        alwaysOnTop = true
    ) {
        onWindow(this.window)
        TasksTheme {
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .width(controller.stackWidth)
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .onGloballyPositioned { coordinates ->
                        val h = with(density) { coordinates.size.height.toDp() }.value
                        if (h > 0f) {
                            if (abs(h - heightDp) > 0.5f) heightDp = h
                            onHeight(h)
                        }
                    }
                    .onPointerEvent(PointerEventType.Scroll, pass = PointerEventPass.Initial) { event ->
                        val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (dy > 0f) controller.scrollBy(SCROLL_STEP)
                        else if (dy < 0f) controller.scrollBy(-SCROLL_STEP)
                    }
            ) { content() }
        }
    }
}

@Composable
private fun NavWindow(
    controller: StackController,
    windowInteractions: WindowInteractions,
    xDp: Float,
    yDp: Float,
    visible: Boolean
) {
    val scope = rememberCoroutineScope()
    SizedWindow(
        controller = controller,
        xDp = xDp,
        yDp = yDp,
        visible = visible,
        focusable = true,
        onHeight = { controller.navHeight = it },
        onWindow = { controller.navWindow = it }
    ) {
        Column(Modifier.fillMaxWidth()) {
            UpdateBanner(windowInteractions)
            Toolbar(
                scope = scope,
                windowInteractions = windowInteractions,
                toolbarInteractions = ToolbarInteractions { controller.showDescription = it }
            )
        }
    }
}

/**
 * Bandeau « nouvelle version disponible » : compare la release GitHub la plus
 * récente à la version embarquée et propose la mise à jour au clic. Repris de
 * l'ancien écran, il ne s'affiche que si une version plus récente existe.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun UpdateBanner(windowInteractions: WindowInteractions) {
    val uriHandler = LocalUriHandler.current
    var latestRelease by remember { mutableStateOf<GithubLatestRelease?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            runCatching {
                val response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(
                        URI.create("https://api.github.com/repos/elfefe/TasksWidget/releases/latest")
                    ).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
                )
                latestRelease = Gson().fromJson(response.body(), GithubLatestRelease::class.java)
            }.onFailure { log(it.stackTraceToString()) }
        }
    }

    val bundledVersion = remember {
        runCatching { useResource("version") { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
    }
    val release = latestRelease
    val newVersion = !dismissed && isRemoteNewer(release?.tagName, bundledVersion)

    if (newVersion && release != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            BasicText(
                text = "New version available: ${release.tagName}",
                style = TextStyle(
                    color = Tasks.Configs.configs.themeColors.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .height(20.dp)
                    .onClick {
                        dismissed = true
                        update(release) { status ->
                            windowInteractions.popup.value = Popup.show(status.message)
                            when (status) {
                                is Updater.Error -> {
                                    log(status.error.stackTraceToString())
                                    (release.htmlUrl ?: release.url)?.let { uriHandler.openUri(it) }
                                }
                                is Updater.Install -> {
                                    delay(500)
                                    windowInteractions.application.exitApplication()
                                }
                                else -> {}
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun TaskWindow(
    controller: StackController,
    windowInteractions: WindowInteractions,
    task: Task,
    xDp: Float,
    yDp: Float,
    visible: Boolean
) {
    SizedWindow(
        controller = controller,
        xDp = xDp,
        yDp = yDp,
        visible = visible,
        focusable = true,
        onHeight = { controller.setHeight(task.created, it) }
    ) {
        TaskCard(Modifier, task, windowInteractions, controller.showDescription)
    }
}

/** Petite poignée blanche sur le bord quand la pile est repliée. */
@Composable
private fun StackHandle(controller: StackController, visible: Boolean) {
    val handleWidth = controller.handleWidth
    val handleHeight = controller.handleHeight
    val x = controller.handleLeft()
    val y = controller.handleTop()

    val state = rememberWindowState(
        position = WindowPosition.Absolute(x.dp, y.dp),
        size = DpSize(handleWidth.dp, handleHeight.dp)
    )
    LaunchedEffect(x, y) {
        state.position = WindowPosition.Absolute(x.dp, y.dp)
    }

    CrashWindow(
        onCloseRequest = { },
        state = state,
        visible = visible,
        title = "Tasks - handle",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true
    ) {
        TasksTheme {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                controller.expanded = true
                            }
                        }
                    }
            ) {
                val barWidth = 5.dp.toPx()
                val left = if (controller.isRight) size.width - barWidth else 0f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(left, size.height * 0.1f),
                    size = Size(barWidth, size.height * 0.8f),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}
