package de.mm20.launcher2.ui.launcher.widgets.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.preferences.ClockWidgetWeatherPill
import de.mm20.launcher2.ui.ktx.atTone

/**
 * Personal build addition (see MODIFICATIONS.md): optional pill behind the text below the clock
 * (weather line, date, alarm, music, battery) for contrast on busy wallpapers.
 *
 * Uses the clock's palette: tones of the theme's primary colour when the clock uses theme colours,
 * plain black / white otherwise.
 *
 * [contentPadding] is the space between the pill's edge and its content. Content that brings its
 * own padding (the parts below the clock) passes less, so that all pills end up the same size.
 */
@Composable
internal fun ClockPill(
    pill: ClockWidgetWeatherPill,
    themed: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ClockPillDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = when (pill) {
        ClockWidgetWeatherPill.None -> null
        ClockWidgetWeatherPill.Light ->
            if (themed) primary.atTone(ClockLightTone).copy(alpha = 0.9f) to primary.atTone(ClockDarkTone)
            else Color.White.copy(alpha = 0.9f) to Color.Black
        ClockWidgetWeatherPill.Dark ->
            if (themed) primary.atTone(ClockDarkTone).copy(alpha = 0.85f) to primary.atTone(ClockLightTone)
            else Color.Black.copy(alpha = 0.75f) to Color.White
    }
    if (colors == null) {
        Box(modifier, contentAlignment = Alignment.Center) { content() }
        return
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.first)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.second) {
            content()
        }
    }
}

/** Personal build: tones of the theme's primary colour for light and dark clock text. */
internal const val ClockLightTone = 90
internal const val ClockDarkTone = 25

internal object ClockPillDefaults {
    val HorizontalPadding = 12.dp
    val VerticalPadding = 4.dp
    val ContentPadding = PaddingValues(horizontal = HorizontalPadding, vertical = VerticalPadding)
}
