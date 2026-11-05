package de.mm20.launcher2.ui.launcher.widgets.clock.parts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Personal build addition (see MODIFICATIONS.md): the two text styles shared by the parts below
 * the clock in the vertical layout (date, music, alarm, battery). A slightly heavier main style
 * (e.g. the song title) and a lighter accent style (e.g. the artist).
 */
internal object ClockPartText {
    val main: TextStyle
        @Composable get() = MaterialTheme.typography.labelLarge

    val accent: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(color = accentColor)

    /** Accent as a span, for inlining in a [main] text. */
    val accentSpan: SpanStyle
        @Composable get() = SpanStyle(fontWeight = FontWeight.Normal, color = accentColor)

    private val accentColor: Color
        @Composable get() = LocalContentColor.current.let { it.copy(alpha = it.alpha * 0.75f) }

    /** Compact padding for a part's tap target, inside the optional pill. */
    val paddingSize = 4.dp
    val padding = Modifier.padding(paddingSize)
}
