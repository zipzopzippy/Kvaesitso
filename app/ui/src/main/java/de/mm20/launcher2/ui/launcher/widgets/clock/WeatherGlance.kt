package de.mm20.launcher2.ui.launcher.widgets.clock

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.ktx.tryStartActivity
import de.mm20.launcher2.preferences.ClockWidgetWeatherPill
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.weather.WeatherIcon
import de.mm20.launcher2.ui.component.weather.WeatherIconDefaults
import de.mm20.launcher2.ui.launcher.widgets.clock.parts.ClockPartText
import de.mm20.launcher2.ui.launcher.widgets.weather.weatherIconById
import de.mm20.launcher2.ui.locals.LocalMeasurementSystem
import de.mm20.launcher2.ui.utils.formatPrecipitation
import de.mm20.launcher2.ui.utils.formatTemperature
import de.mm20.launcher2.weather.Forecast
import de.mm20.launcher2.weather.WeatherSummary
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Personal build addition (see MODIFICATIONS.md): a weather line below the clock, using the summary
 * of the active weather provider. Hidden if the provider doesn't offer one.
 *
 * During the day it shows the current weather ("Now"), then today and tonight. At night it shows
 * the current weather, then tonight and the next day. Night starts when the hourly forecast for the
 * current hour is marked as night (Breezy: not daylight). The labels use the lighter of the two clock part
 * text styles, the temperatures the heavier one.
 *
 * A raindrop with the total adds up the next 12 hours, if any is expected, shown as "<1 mm" below
 * 1 mm. A snowflake replaces the raindrop if any of it is snow.
 */
@Composable
fun WeatherGlance(
    summary: WeatherSummary?,
    hours: List<Forecast>,
    /** The current time, rounded to the minute so that this doesn't rerun on every clock tick. */
    time: Long,
    showPrecipitation: Boolean,
    modifier: Modifier = Modifier,
    pill: ClockWidgetWeatherPill = ClockWidgetWeatherPill.None,
    themed: Boolean = false,
) {
    val glance = remember(summary, hours, time) {
        summary?.let { glanceFor(it, hours, time) }
    }
    // Personal build: slides in (pushing the clock / dynamic zone aside) when the weather loads,
    // which can take a while right after boot. The last glance stays on screen while it slides out.
    var shownGlance by remember { mutableStateOf(glance) }
    if (glance != null && glance != shownGlance) shownGlance = glance
    AnimatedVisibility(
        visible = glance != null,
        enter = ClockLineEnterTransition,
        exit = ClockLineExitTransition,
    ) {
        shownGlance?.let { WeatherGlanceContent(it, showPrecipitation, modifier, pill, themed) }
    }
}

@Composable
private fun WeatherGlanceContent(
    glance: Glance,
    showPrecipitation: Boolean,
    modifier: Modifier,
    pill: ClockWidgetWeatherPill,
    themed: Boolean,
) {
    val context = LocalContext.current
    val measurementSystem = LocalMeasurementSystem.current

    ClockPill(pill = pill, themed = themed, modifier = modifier) {
        Row(
            modifier = Modifier.clickable { openWeatherApp(context) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (period in glance.periods) {
                IconWithText(
                    text = formatTemperature(context, period.temperature.toFloat(), measurementSystem),
                    label = period.label?.let { stringResource(it) },
                ) {
                    SmallWeatherIcon(period.icon, period.night)
                }
            }
            if (showPrecipitation && glance.precipitation != null) {
                val millimeters = glance.precipitation.millimeters
                IconWithText(
                    if (millimeters < MinPrecipitationMillimeters) {
                        "<" + formatPrecipitation(context, MinPrecipitationMillimeters.toFloat(), measurementSystem)
                    } else {
                        formatPrecipitation(context, millimeters.roundToInt().toFloat(), measurementSystem)
                    }
                ) {
                    // The weather icon's palette. These symbols don't fill their box, so only take
                    // up the width of the shape: the gap to the text then looks the same.
                    val iconColors = WeatherIconDefaults.colors()
                    Icon(
                        painterResource(
                            if (glance.precipitation.snow) R.drawable.ac_unit_24px
                            else R.drawable.water_drop_24px_filled
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .width(if (glance.precipitation.snow) 13.dp else 11.dp)
                            .requiredSize(IconSize),
                        tint = if (glance.precipitation.snow) iconColors.snow else iconColors.rain,
                    )
                }
            }
        }
    }
}

/** [WeatherIcon] is made of layers with fixed sizes, so it has to be scaled as a whole. */
@Composable
private fun SmallWeatherIcon(icon: Int, night: Boolean) {
    Box(modifier = Modifier.size(IconSize), contentAlignment = Alignment.Center) {
        WeatherIcon(
            icon = weatherIconById(icon),
            night = night,
            modifier = Modifier
                .requiredSize(WeatherIconFullSize)
                .scale(IconSize / WeatherIconFullSize),
        )
    }
}

@Composable
private fun IconWithText(text: String, label: String? = null, icon: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label != null) {
            Text(text = label, style = ClockPartText.accent)
        }
        icon()
        Text(text = text, style = ClockPartText.main)
    }
}

/**
 * Opens the device's weather app. If there are several (apps in the weather category, plus Breezy
 * Weather), the system's app picker asks which one.
 *
 * Weather apps only put the weather category on their launcher entry, without the default category
 * an implicit start needs, so the apps are looked up and then started by their component.
 */
private fun openWeatherApp(context: Context) {
    val pm = context.packageManager
    val categoryApps = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_WEATHER), 0
    ).map { Intent(Intent.ACTION_MAIN).setClassName(it.activityInfo.packageName, it.activityInfo.name) }
    val breezy = pm.getLaunchIntentForPackage(BreezyWeatherPackage)
        ?.takeIf { categoryApps.none { app -> app.component?.packageName == BreezyWeatherPackage } }
    val apps = categoryApps + listOfNotNull(breezy)
    val intent = when (apps.size) {
        0 -> return
        1 -> apps.first()
        else -> Intent.createChooser(apps.first(), null)
            .putExtra(Intent.EXTRA_INITIAL_INTENTS, apps.drop(1).toTypedArray())
    }
    context.tryStartActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private const val BreezyWeatherPackage = "org.breezyweather"
private val IconSize = 16.dp
private val WeatherIconFullSize = 24.dp
private const val PrecipitationWindowMillis = 12 * 60 * 60 * 1000L
private const val HourMillis = 60 * 60 * 1000L
private const val MinPrecipitationMillimeters = 1.0

private data class GlancePeriod(
    val icon: Int,
    val temperature: Double,
    val night: Boolean,
    @StringRes val label: Int? = null,
)

private data class UpcomingPrecipitation(val millimeters: Double, val snow: Boolean)

private data class Glance(
    val periods: List<GlancePeriod>,
    val precipitation: UpcomingPrecipitation?,
)

private fun glanceFor(summary: WeatherSummary, hours: List<Forecast>, now: Long): Glance? {
    val current = summary.current ?: return null
    // The forecasts can start at the next full hour, right after an update.
    val isNight = (hours.lastOrNull { it.timestamp <= now } ?: hours.firstOrNull())?.night == true
    // A night belongs to the date it starts on, so before noon it started yesterday.
    val isMorning = Calendar.getInstance().apply { timeInMillis = now }
        .get(Calendar.HOUR_OF_DAY) < 12

    fun day(offset: Int) = summary.days.firstOrNull { isSameDay(it.date, now, offset) }

    val forecast = when {
        !isNight -> listOf(
            day(0)?.day.asPeriod(false, R.string.date_today),
            day(0)?.night.asPeriod(true, R.string.date_tonight),
        )
        isMorning -> listOf(
            day(-1)?.night.asPeriod(true, R.string.date_tonight),
            day(0)?.day.asPeriod(false, R.string.date_today),
        )
        else -> listOf(
            day(0)?.night.asPeriod(true, R.string.date_tonight),
            day(1)?.day.asPeriod(false, R.string.date_tomorrow),
        )
    }
    return Glance(
        periods = listOfNotNull(current.asPeriod(isNight, R.string.date_now)) + forecast.filterNotNull(),
        precipitation = upcomingPrecipitation(hours, now),
    )
}

private fun WeatherSummary.Period?.asPeriod(night: Boolean, @StringRes label: Int? = null): GlancePeriod? {
    return this?.let { GlancePeriod(it.icon, it.temperature, night, label) }
}

/** The expected total for the next 12 hours (hourly forecasts in mm), if any is expected. */
private fun upcomingPrecipitation(forecasts: List<Forecast>, now: Long): UpcomingPrecipitation? {
    val hours = forecasts
        .filter { it.timestamp + HourMillis > now && it.timestamp < now + PrecipitationWindowMillis }
    val total = hours.sumOf { it.precipitation ?: 0.0 }
    if (total <= 0.0) return null
    return UpcomingPrecipitation(
        millimeters = total,
        // Not every weather source splits out snow, so also go by the hour's condition.
        snow = hours.any {
            (it.snow ?: 0.0) > 0.0 || (it.icon == Forecast.SNOW && (it.precipitation ?: 0.0) > 0.0)
        },
    )
}

private fun isSameDay(timestamp: Long, now: Long, dayOffset: Int): Boolean {
    val target = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, dayOffset)
    }
    val day = Calendar.getInstance().apply { timeInMillis = timestamp }
    return day.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            day.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
