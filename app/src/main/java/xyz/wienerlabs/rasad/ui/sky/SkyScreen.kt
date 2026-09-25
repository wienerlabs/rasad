package xyz.wienerlabs.rasad.ui.sky

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.sky.ObjectDescriber
import xyz.wienerlabs.rasad.sky.ObjectDetails
import xyz.wienerlabs.rasad.sky.OrientationTracker
import xyz.wienerlabs.rasad.sky.SkyController
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import xyz.wienerlabs.rasad.sky.SkyRenderer
import xyz.wienerlabs.rasad.sky.SkyTypefaces
import xyz.wienerlabs.rasad.sky.ViewMode
import xyz.wienerlabs.rasad.sky.ViewingPlan
import xyz.wienerlabs.rasad.sky.VisibilityPlanner
import xyz.wienerlabs.rasad.sky.equatorialDirection
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

@Composable
fun SkyScreen(
    controller: SkyController,
    typefaces: SkyTypefaces,
    searchIndex: List<SearchEntry>,
    placeName: String?,
    nightVision: Boolean,
    onNightVisionChange: (Boolean) -> Unit,
    onOpenHilal: () -> Unit,
    showOnboarding: Boolean,
    onOnboardingSeen: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val renderer = remember(controller.catalog, typefaces) { SkyRenderer(controller.catalog, typefaces, density.density) }
    val tracker = remember { OrientationTracker(context) }
    var timeOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var layersOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    val tapRadius = with(density) { 30.dp.toPx() }
    val tapWeight = with(density) { 2.dp.toPx() }

    LifecycleResumeEffect(controller.mode, tracker) {
        if (controller.mode == ViewMode.Sensor) tracker.start()
        onPauseOrDispose { tracker.stop() }
    }

    LaunchedEffect(controller.location) {
        tracker.updateDeclination(controller.location, System.currentTimeMillis())
    }

    DisposableEffect(controller, view) {
        controller.onQiblaAligned = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
        controller.onTargetCentered = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
        controller.onTargetInView = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        onDispose {
            controller.onQiblaAligned = null
            controller.onTargetCentered = null
            controller.onTargetInView = null
        }
    }

    LaunchedEffect(controller, tracker) {
        while (true) {
            withFrameNanos { controller.onFrame(it, tracker) }
        }
    }

    LaunchedEffect(showOnboarding) {
        if (showOnboarding) {
            delay(7000)
            onOnboardingSeen()
        }
    }

    BackHandler(enabled = aboutOpen || searchOpen || layersOpen || timeOpen || controller.selected != null || controller.target != null) {
        when {
            aboutOpen -> aboutOpen = false
            searchOpen -> searchOpen = false
            layersOpen -> layersOpen = false
            controller.selected != null -> controller.selected = null
            timeOpen -> timeOpen = false
            else -> controller.target = null
        }
    }

    val onTouch by rememberUpdatedState {
        layersOpen = false
        if (showOnboarding) onOnboardingSeen()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .skyGestures(controller, tapRadius, tapWeight) { onTouch() },
        ) {
            controller.frame
            controller.camera.setViewport(size.width, size.height)
            drawIntoCanvas { canvas ->
                renderer.render(canvas.nativeCanvas, controller.camera, controller.current, controller.renderState, controller.picks)
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(Palette.Ink.copy(alpha = 0.62f), Palette.Ink.copy(alpha = 0f)))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(190.dp)
                .background(Brush.verticalGradient(listOf(Palette.Ink.copy(alpha = 0f), Palette.Ink.copy(alpha = 0.55f)))),
        )

        ClockHeader(controller, placeName, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 20.dp, top = 10.dp))

        Row(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(RasadIcons.Eye, "Gece görüşü", { onNightVisionChange(!nightVision) }, active = nightVision)
            RoundIconButton(RasadIcons.Layers, "Katmanlar", { layersOpen = !layersOpen }, active = layersOpen)
        }

        AnimatedVisibility(
            visible = layersOpen,
            enter = fadeIn() + slideInVertically { -it / 6 },
            exit = fadeOut() + slideOutVertically { -it / 6 },
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 66.dp, end = 16.dp),
        ) {
            LayersPanel(
                layers = controller.layers,
                nightVision = nightVision,
                onLayersChange = { controller.layers = it },
                onNightVisionChange = onNightVisionChange,
                onOpenAbout = {
                    layersOpen = false
                    aboutOpen = true
                },
            )
        }

        QiblaBanner(controller, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 110.dp))

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (controller.selected == null) GuidancePanel(controller)
            PointingReadout(controller)
            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(visible = timeOpen, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
                TimeDial(controller.clock, { controller.frame }, typefaces.sans, Modifier.padding(bottom = 10.dp))
            }
            Dock(
                mode = controller.mode,
                timeOpen = timeOpen,
                onMode = {
                    if (controller.mode == ViewMode.Sensor) {
                        controller.switchToManual()
                    } else if (tracker.isAvailable) {
                        controller.switchToSensor()
                    } else {
                        Toast.makeText(context, "Bu cihazda yön sensörü bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                },
                onTime = { timeOpen = !timeOpen },
                onSearch = { searchOpen = true },
                onHilal = onOpenHilal,
            )
        }

        AnimatedVisibility(
            visible = controller.selected != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SelectedObjectSheet(controller)
        }

        AnimatedVisibility(visible = showOnboarding && controller.mode == ViewMode.Sensor, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
            Onboarding()
        }

        AnimatedVisibility(visible = aboutOpen, enter = fadeIn(), exit = fadeOut()) {
            AboutPanel(onDismiss = { aboutOpen = false })
        }

        AnimatedVisibility(visible = searchOpen, enter = fadeIn(), exit = fadeOut()) {
            val snapshot = controller.published
            val statuses = remember(searchIndex, snapshot.millis / 60_000L, snapshot.location) {
                skyStatuses(searchIndex, controller.catalog, snapshot, controller.qiblaAzimuth)
            }
            val highlights = remember(statuses) { skyHighlights(searchIndex, controller.catalog, snapshot, statuses) }
            SearchPanel(
                index = searchIndex,
                statuses = statuses,
                highlights = highlights,
                onSelect = { ref ->
                    searchOpen = false
                    controller.selected = ref
                    controller.flyTo(ref)
                },
                onDismiss = { searchOpen = false },
            )
        }
    }
}

private fun Modifier.skyGestures(controller: SkyController, tapRadius: Float, tapWeight: Float, onTouch: () -> Unit): Modifier =
    pointerInput(controller) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onTouch()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var dragging = false
            var multiTouch = false
            var travelled = 0f
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                if (pressed.size > 1) {
                    multiTouch = true
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (zoom != 1f) controller.onZoom(zoom)
                    if (pan != Offset.Zero) controller.onPan(pan.x, pan.y)
                    event.changes.forEach { it.consume() }
                } else {
                    val change = pressed.first()
                    val delta = change.positionChange()
                    travelled += delta.getDistance()
                    if (!dragging && travelled > viewConfiguration.touchSlop) dragging = true
                    if (dragging && !multiTouch) {
                        controller.onPan(delta.x, delta.y)
                        change.consume()
                    }
                    tracker.addPosition(change.uptimeMillis, change.position)
                }
            }
            if (!dragging && !multiTouch) {
                controller.onTap(down.position.x, down.position.y, tapRadius, tapWeight)
            } else if (dragging && !multiTouch) {
                val velocity = tracker.calculateVelocity()
                controller.onFling(velocity.x, velocity.y)
            }
        }
    }

@Composable
private fun ClockHeader(controller: SkyController, placeName: String?, modifier: Modifier = Modifier) {
    val minute = controller.displayedMinute
    val millis = minute * 60_000L
    val clock = controller.clock
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Formats.clock(millis), style = RasadType.clock, color = Palette.Text)
            if (!clock.isLive) {
                Spacer(Modifier.size(10.dp))
                Pill(
                    text = "Şimdiye dön",
                    onClick = { clock.reset() },
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        Text(Formats.dayMonthWeekday(millis), style = RasadType.label, color = Palette.TextMuted)
        Text(
            listOfNotNull(placeName, controller.location.formatted()).joinToString(" · "),
            style = RasadType.caption,
            color = Palette.TextFaint,
        )
    }
}

@Composable
private fun PointingReadout(controller: SkyController) {
    val azimuth = controller.centerAzimuth.toDouble()
    val altitude = controller.centerAltitude.toDouble()
    val text = buildString {
        append(Formats.degrees(azimuth))
        append(' ')
        append(Formats.compassPoint(azimuth))
        append(" · ")
        append(Formats.degrees(altitude).replace('-', '−'))
        append(" yükseklik")
        if (controller.mode == ViewMode.Manual) {
            append(" · görüş ")
            append(Formats.degrees(controller.camera.fovDegrees))
        }
    }
    Text(text, style = RasadType.caption, color = Palette.TextMuted)
}

@Composable
private fun QiblaBanner(controller: SkyController, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = controller.qiblaAligned, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut(), modifier = modifier) {
        Row(
            Modifier.panel(RoundedCornerShape(50), strong = true).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(RasadIcons.Kaaba, contentDescription = null, tint = Palette.Text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text("Kıble yönündesin · ${Formats.degrees(controller.qiblaAzimuth, 1)}", style = RasadType.label, color = Palette.Text)
        }
    }
}

@Composable
private fun GuidancePanel(controller: SkyController) {
    val readout = controller.guidance ?: return
    val target = readout.ref
    val location = controller.location
    val hourKey = controller.displayedMinute / 60L
    val plan by produceState<ViewingPlan?>(null, target, location, hourKey) {
        val snapshot = controller.published
        value = withContext(Dispatchers.Default) {
            runCatching {
                target.equatorialDirection(controller.catalog, snapshot)?.let { VisibilityPlanner.plan(it, location, snapshot.millis) }
            }.getOrNull()
        }
    }
    GuidanceCard(
        readout = readout,
        plan = plan,
        nowMillis = controller.displayedMinute * 60_000L,
        onJump = { controller.jumpTo(it) },
        onClose = { controller.target = null },
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun SelectedObjectSheet(controller: SkyController) {
    val selected = controller.selected ?: return
    val snapshot = controller.published
    val location = controller.location
    val minuteKey = snapshot.millis / 60_000L
    val details by produceState<ObjectDetails?>(null, selected, location, minuteKey) {
        value = withContext(Dispatchers.Default) {
            runCatching { ObjectDescriber.describe(selected, controller.catalog, snapshot, location) }.getOrNull()
        }
    }
    ObjectSheet(
        details = details,
        isTarget = controller.target == selected,
        onDismiss = { controller.selected = null },
        onTarget = {
            if (controller.target == selected) controller.target = null else controller.flyTo(selected)
        },
        onRelated = { ref ->
            controller.selected = ref
            controller.flyTo(ref)
        },
        onJump = { millis ->
            controller.target = selected
            controller.jumpTo(millis)
        },
    )
}

@Composable
private fun Dock(
    mode: ViewMode,
    timeOpen: Boolean,
    onMode: () -> Unit,
    onTime: () -> Unit,
    onSearch: () -> Unit,
    onHilal: () -> Unit,
) {
    Row(
        Modifier.widthIn(max = 420.dp).fillMaxWidth().panel(RoundedCornerShape(26.dp)).padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DockItem(
            icon = if (mode == ViewMode.Sensor) RasadIcons.Sensor else RasadIcons.Hand,
            label = if (mode == ViewMode.Sensor) "Pusula" else "Serbest",
            active = mode == ViewMode.Sensor,
            onClick = onMode,
        )
        DockItem(RasadIcons.Clock, "Zaman", timeOpen, onTime)
        DockItem(RasadIcons.Search, "Ara", false, onSearch)
        DockItem(RasadIcons.Moon, "Hilal", false, onHilal)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DockItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .panelIf(active)
            .pressable(onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Palette.Text else Palette.TextMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = RasadType.caption, color = if (active) Palette.Text else Palette.TextMuted)
    }
}

private fun Modifier.panelIf(active: Boolean): Modifier =
    if (active) this.padding(horizontal = 2.dp).panel(RoundedCornerShape(20.dp)) else this

@Composable
private fun Onboarding() {
    Column(
        Modifier.padding(32.dp).panel(RoundedCornerShape(26.dp), strong = true).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(RasadIcons.Sensor, contentDescription = null, tint = Palette.Text, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(12.dp))
        Text("Telefonunu göğe doğru tut", style = RasadType.title, color = Palette.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Yıldızlar, gezegenler ve kıble gerçek yerlerinde belirir. Elle gezinmek için ekranı kaydır, yakınlaşmak için iki parmağını aç.",
            style = RasadType.body,
            color = Palette.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}
