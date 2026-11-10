package de.mm20.launcher2.ui.launcher.widgets.clock

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.ktx.isAtLeastApiLevel
import de.mm20.launcher2.ktx.tryStartActivity
import de.mm20.launcher2.preferences.ui.ClockWidgetSettings
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.AlarmPartProvider
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.BatteryPartProvider
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.DatePartProvider
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.FavoritesPartProvider
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.MusicPartProvider
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.PartProvider
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.SmartspacerPartProvider
import de.mm20.launcher2.weather.Forecast
import de.mm20.launcher2.weather.WeatherRepository
import de.mm20.launcher2.weather.WeatherSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClockWidgetVM : ViewModel(), KoinComponent {
    private val settings: ClockWidgetSettings by inject()
    private val weatherRepository: WeatherRepository by inject()

    /**
     * Personal build: on a cold start (right after boot the device is busy), the dynamic zone and
     * the weather start loading a little after the clock, one after the other, so they aren't all
     * competing for the device at once: the dynamic zone [StaggerDelay] after this view model was
     * created, the weather [StaggerDelay] after the dynamic zone has shown its first part. The
     * delays are counted from those moments, so later subscriptions start right away.
     */
    private val createdAt = SystemClock.uptimeMillis()

    /** When the dynamic zone first loaded (uptime millis). */
    private val partLoadedAt = CompletableDeferred<Long>()

    private fun <T> Flow<T>.startingAfterPart(): Flow<T> = flow {
        // Don't hold the weather back for long if the dynamic zone is stuck
        val loadedAt = withTimeoutOrNull(createdAt + MaxWeatherWait - SystemClock.uptimeMillis()) {
            partLoadedAt.await()
        } ?: SystemClock.uptimeMillis()
        delay(loadedAt + StaggerDelay - SystemClock.uptimeMillis())
        emitAll(this@startingAfterPart)
    }

    private val partProviders = settings.parts.combine(settings.useSmartspacer) { p, s ->
        p to s
    }.map { (parts, smartspacer) ->
        if (smartspacer && isAtLeastApiLevel(29)) {
            return@map listOf(SmartspacerPartProvider())
        }

        val providers = mutableListOf<PartProvider>()
        if (parts.date) providers += DatePartProvider()
        if (parts.music) providers += MusicPartProvider()
        providers += BatteryPartProvider(parts.battery)
        if (parts.alarm) providers += AlarmPartProvider()
        providers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private var activePart: StateFlow<PartProvider?>? = null

    /**
     * Personal build: kept in the view model, so that the clock widget leaving and re-entering
     * composition (e.g. edit mode) doesn't briefly lose the part and animate it in again.
     */
    fun getActivePart(context: Context): StateFlow<PartProvider?> = activePart
        ?: createActivePart(context.applicationContext)
            .onEach { partLoadedAt.complete(SystemClock.uptimeMillis()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
            .also { activePart = it }

    private fun createActivePart(context: Context): Flow<PartProvider?> = channelFlow {
        delay(createdAt + StaggerDelay - SystemClock.uptimeMillis())
        partProviders.collectLatest { providers ->
            // Personal build: only empty before the settings have loaded (there's always the
            // battery part), so don't count that as the dynamic zone having loaded
            if (providers.isEmpty()) return@collectLatest
            val rankings = providers.map { it.getRanking(context).map { r -> r to it } }
            combine(rankings) { r ->
                r.filter { it.first > 0 }.maxByOrNull { it.first }?.second
            }.collectLatest {
                send(it)
            }
        }
    }

    val compactLayout = settings.compact
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val clockStyle = settings.clockStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val color = settings.color
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val alignment = settings.alignment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val dockProvider = settings.dock
        .map { if (it) FavoritesPartProvider() else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val textShadow = settings.textShadow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val weather = settings.weather
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val weatherPrecipitation = settings.weatherPrecipitation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val useThemeColor = settings.useThemeColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val weatherPill = settings.weatherPill
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), de.mm20.launcher2.preferences.ClockWidgetWeatherPill.None)

    val dockAboveSearchBar = settings.dockAboveSearchBar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Personal build: the weather line's data, kept here for the same reason as the active part
    val weatherSummary: StateFlow<WeatherSummary?> = weatherRepository.getSummary()
        .startingAfterPart()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weatherForecasts: StateFlow<List<Forecast>> = weatherRepository.getForecasts()
        .startingAfterPart()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTime(time: Long) {
        partProviders.value.forEach { it.setTime(time) }
    }

    fun launchClockApp(context: Context) {
        context.tryStartActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

private const val StaggerDelay = 250L
private const val MaxWeatherWait = 2000L
