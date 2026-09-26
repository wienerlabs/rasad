package xyz.wienerlabs.rasad

import android.Manifest
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.location.LocationSource
import xyz.wienerlabs.rasad.sky.SkyCatalog
import xyz.wienerlabs.rasad.sky.SkyClock
import xyz.wienerlabs.rasad.sky.SkyController
import xyz.wienerlabs.rasad.sky.SkyTypefaces
import xyz.wienerlabs.rasad.islam.IslamicPreferences
import xyz.wienerlabs.rasad.ui.islam.CalendarScreen
import xyz.wienerlabs.rasad.ui.islam.CalendarTab
import xyz.wienerlabs.rasad.ui.sky.SkyScreen
import xyz.wienerlabs.rasad.ui.sky.SearchEntry
import xyz.wienerlabs.rasad.ui.sky.buildSearchIndex
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadTheme
import xyz.wienerlabs.rasad.ui.theme.RasadType

private enum class Screen { Sky, Calendar }

private class LoadedAssets(val catalog: SkyCatalog, val typefaces: SkyTypefaces)

private val nightVisionMatrix = floatArrayOf(
    0.42f, 0.72f, 0.20f, 0f, 0f,
    0f, 0f, 0f, 0f, 0f,
    0f, 0f, 0f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private fun Modifier.nightVision(enabled: Boolean): Modifier =
    if (!enabled) this else graphicsLayer {
        renderEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(nightVisionMatrix)).asComposeRenderEffect()
    }

@Composable
fun RasadApp(request: LaunchRequest?) {
    val context = LocalContext.current
    val locationSource = remember { LocationSource(context) }
    val preferences = remember { context.getSharedPreferences("rasad", Context.MODE_PRIVATE) }
    val islamicPreferences = remember { IslamicPreferences(preferences) }
    val saved = remember { locationSource.saved() }
    var assets by remember { mutableStateOf<LoadedAssets?>(null) }
    var location by remember { mutableStateOf(saved ?: GeoPoint.Istanbul) }
    var placeName by remember { mutableStateOf(if (saved == null) "İstanbul" else locationSource.savedPlaceName()) }
    var locationLocked by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(Screen.Sky) }
    var nightVision by remember { mutableStateOf(false) }
    var onboarding by remember { mutableStateOf(!preferences.getBoolean("onboarded", false)) }
    var locateRequests by remember { mutableIntStateOf(0) }
    var calendarMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var calendarTab by remember { mutableStateOf(CalendarTab.Hilal) }
    var hilalEvening by remember { mutableStateOf<Int?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locateRequests++
    }

    LaunchedEffect(Unit) {
        assets = withContext(Dispatchers.IO) {
            LoadedAssets(
                catalog = SkyCatalog.load(context),
                typefaces = SkyTypefaces(context.resources.getFont(R.font.funnel_sans), context.resources.getFont(R.font.funnel_display)),
            )
        }
    }

    LaunchedEffect(Unit) {
        if (request?.hasLocation == true) return@LaunchedEffect
        if (locationSource.hasPermission()) {
            locateRequests++
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    LaunchedEffect(locateRequests) {
        if (locateRequests == 0 || locationLocked) return@LaunchedEffect
        val fix = locationSource.current() ?: return@LaunchedEffect
        if (locationLocked) return@LaunchedEffect
        location = fix
        val name = locationSource.placeName(fix)
        placeName = name
        locationSource.save(fix, name)
    }

    RasadTheme {
        Box(Modifier.fillMaxSize().background(Palette.Ink).nightVision(nightVision)) {
            val ready = assets
            if (ready == null) {
                Wordmark(Modifier.align(Alignment.Center))
                return@Box
            }
            val loaded = ready.catalog
            val typefaces = ready.typefaces
            val clock = remember { SkyClock() }
            val controller = remember(loaded) { SkyController(loaded, clock, location) }
            val searchIndex by produceState(emptyList<SearchEntry>(), loaded) {
                value = withContext(Dispatchers.Default) { buildSearchIndex(loaded) }
            }

            LaunchedEffect(location) { controller.location = location }

            LaunchedEffect(request?.id, loaded) {
                val incoming = request ?: return@LaunchedEffect
                if (incoming.hasLocation) {
                    locationLocked = true
                    location = GeoPoint(incoming.latitude!!, incoming.longitude!!)
                    placeName = incoming.place
                }
                incoming.timeMillis?.let { clock.setAbsolute(it) }
                clock.playing = incoming.play
                controller.refreshNow()
                if (incoming.quiet) onboarding = false
                if (incoming.night) nightVision = true
                if (incoming.grid) controller.layers = controller.layers.copy(grid = true)
                if (incoming.azimuth != null) {
                    controller.lookAt(incoming.azimuth, incoming.altitude ?: 25.0, incoming.fov)
                } else if (incoming.fov != null) {
                    controller.camera.setFov(incoming.fov)
                }
                controller.selected = incoming.select?.let { loaded.resolve(it) }
                controller.target = incoming.target?.let { loaded.resolve(it) }
                if (incoming.screen == "hilal" || incoming.screen == "takvim") {
                    calendarMillis = clock.now()
                    hilalEvening = incoming.evening
                    calendarTab = CalendarTab.fromKey(incoming.tab) ?: CalendarTab.Hilal
                    screen = Screen.Calendar
                } else if (incoming.screen == "sky") {
                    screen = Screen.Sky
                }
            }

            Crossfade(targetState = screen, animationSpec = tween(420), label = "screen") { current ->
                when (current) {
                    Screen.Sky -> SkyScreen(
                        controller = controller,
                        typefaces = typefaces,
                        searchIndex = searchIndex,
                        placeName = placeName,
                        nightVision = nightVision,
                        onNightVisionChange = { nightVision = it },
                        islamicPreferences = islamicPreferences,
                        onOpenCalendar = {
                            calendarMillis = System.currentTimeMillis()
                            hilalEvening = null
                            screen = Screen.Calendar
                        },
                        showOnboarding = onboarding,
                        onOnboardingSeen = {
                            onboarding = false
                            preferences.edit().putBoolean("onboarded", true).apply()
                        },
                    )
                    Screen.Calendar -> CalendarScreen(
                        millis = calendarMillis,
                        location = location,
                        placeName = placeName,
                        catalog = loaded,
                        preferences = islamicPreferences,
                        tab = calendarTab,
                        onTabChange = { calendarTab = it },
                        initialEvening = hilalEvening,
                        onBack = { screen = Screen.Sky },
                        onShowInSky = { presentation ->
                            controller.present(presentation)
                            screen = Screen.Sky
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Rasad", style = RasadType.hero, color = Palette.Text)
        Spacer(Modifier.height(6.dp))
        Text("cebindeki rasathane", style = RasadType.label, color = Palette.TextMuted)
    }
}
