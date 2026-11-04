package de.mm20.launcher2.ui.launcher.widgets.clock.parts

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.icu.text.DateFormat
import android.icu.util.ULocale
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.ktx.tryStartActivity
import de.mm20.launcher2.preferences.search.CalendarSearchSettings
import de.mm20.launcher2.search.CalendarEvent
import de.mm20.launcher2.ui.base.LocalTime
import de.mm20.launcher2.ui.locals.LocalCalendarSystems
import de.mm20.launcher2.ui.locals.LocalTimeFormat
import de.mm20.launcher2.ui.utils.isTwentyFourHours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.koin.compose.koinInject
import java.util.*

class DatePartProvider : PartProvider {
    override fun getRanking(context: Context): Flow<Int> = flow {
        emit(1)
    }

    @Composable
    override fun Component(compactLayout: Boolean) {
        val time = LocalTime.current
        val date = Date(time)
        val verticalLayout = !compactLayout
        val context = LocalContext.current

        val calendars = LocalCalendarSystems.current

        val primaryCal = calendars.first()
        val secondaryCal = calendars.getOrNull(1)

        // Personal build: compact date in the shared clock part style (see MODIFICATIONS.md)
        if (verticalLayout) {
            Column(
                modifier = Modifier.clickable { openCalendar(context) }.then(ClockPartText.padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = DateFormat.getInstanceForSkeleton(primaryCal, "EEEE, MMMM d, yyyy", ULocale.getDefault()).format(date),
                    style = ClockPartText.main,
                )
                if (secondaryCal != null) {
                    Text(
                        text = DateFormat.getInstanceForSkeleton(secondaryCal, "MMMM dd, yyyy", ULocale.getDefault()).format(date),
                        style = ClockPartText.accent,
                    )
                }
                TodayEvent(time - time % 60_000L, primaryCal)
            }
            return
        }

        TextButton(
            colors = ButtonDefaults.textButtonColors(
                contentColor = LocalContentColor.current
            ),
            onClick = {
                val startMillis = System.currentTimeMillis()
                val builder = CalendarContract.CONTENT_URI.buildUpon()
                builder.appendPath("time")
                ContentUris.appendId(builder, startMillis)
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(builder.build())
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.tryStartActivity(intent)
            }) {
            if (verticalLayout) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DateFormat.getInstanceForSkeleton(
                            primaryCal,
                            "EEEE, MMMM d, yyyy",
                            ULocale.getDefault(),
                        ).format(date),
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (secondaryCal != null) {
                        Text(
                            text = DateFormat.getInstanceForSkeleton(
                                secondaryCal,
                                "MMMM dd, yyyy",
                                ULocale.getDefault(),
                            ).format(date),
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(
                                LocalContentColor.current.alpha * 0.7f
                            )
                        )
                    }
                }
            } else {
                if (secondaryCal == null) {
                    val line1 = DateFormat.getInstanceForSkeleton(
                        primaryCal,
                        "EEEE",
                        ULocale.getDefault(),
                    ).format(date)
                    val line2 =
                        DateFormat.getInstanceForSkeleton(
                            primaryCal,
                            "MMMM d",
                            ULocale.getDefault(),
                        ).format(date)
                    Text(
                        text = "$line1\n$line2",
                        lineHeight = 1.2.em,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                        )
                    )
                } else {
                    val line2 =
                        DateFormat.getInstanceForSkeleton(
                            secondaryCal,
                            "MMMM d",
                            ULocale.getDefault(),
                        ).format(date)
                    Column {
                        Text(
                            text = DateFormat.getInstanceForSkeleton(
                                primaryCal,
                                "E MMMM d",
                                ULocale.getDefault(),
                            ).format(date),
                            lineHeight = 1.2.em,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = DateFormat.getInstanceForSkeleton(
                                secondaryCal,
                                "MMMM d",
                                ULocale.getDefault(),
                            ).format(date),
                            lineHeight = 1.2.em,
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalContentColor.current.copy(
                                LocalContentColor.current.alpha * 0.7f
                            )
                        )
                    }
                }
            }
        }
    }

}

// Personal build: how often today's events are re-read, so edits in the calendar show up
private const val EventRefreshInterval = 5 * 60_000L

/**
 * Personal build: the next timed event today ("Name · 3:00 pm"), else an all-day event ("Name").
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun TodayEvent(now: Long, primaryCal: android.icu.util.Calendar?) {
    val calendarRepository: CalendarRepository = koinInject()
    val calendarSettings: CalendarSearchSettings = koinInject()
    val (dayStart, dayEnd) = remember(now) { todayBounds(now) }

    val events by remember(dayStart, now / EventRefreshInterval) {
        calendarSettings.excludedCalendars.flatMapLatest { excluded ->
            calendarRepository.findMany(
                from = dayStart,
                to = dayEnd,
                excludeCalendars = excluded.toList(),
            )
        }
    }.collectAsState(emptyList())

    val event = remember(events, now) {
        nextEventToday(events, now, dayStart, dayEnd)
    } ?: return

    val twentyFourHours = LocalTimeFormat.current.isTwentyFourHours(LocalContext.current)
    // Date formatters are expensive to create, so only do this when the event changes.
    val text = remember(event, twentyFourHours, primaryCal) {
        val name = event.labelOverride ?: event.label
        if (event.allDay) {
            name
        } else {
            val start = DateFormat
                .getInstanceForSkeleton(primaryCal, if (twentyFourHours) "Hm" else "hm", ULocale.getDefault())
                .format(Date(event.startTime!!))
            "$name · $start"
        }
    }
    Text(
        text = text,
        style = ClockPartText.accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun nextEventToday(
    events: List<CalendarEvent>,
    now: Long,
    dayStart: Long,
    dayEnd: Long,
): CalendarEvent? {
    // The calendar's day range is off by the UTC offset for all-day events, so re-check it here.
    val today = events.filter { event ->
        val start = event.startTime
        !event.isTask && start != null && start < dayEnd && event.endTime >= dayStart
    }
    return today.filter { !it.allDay && it.startTime!! > now }.minByOrNull { it.startTime!! }
        ?: today.firstOrNull { it.allDay }
}

private fun todayBounds(now: Long): Pair<Long, Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    return start to calendar.timeInMillis
}

// Personal build: opens the calendar app at today
private fun openCalendar(context: Context) {
    val builder = CalendarContract.CONTENT_URI.buildUpon()
    builder.appendPath("time")
    ContentUris.appendId(builder, System.currentTimeMillis())
    context.tryStartActivity(Intent(Intent.ACTION_VIEW).setData(builder.build()).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
