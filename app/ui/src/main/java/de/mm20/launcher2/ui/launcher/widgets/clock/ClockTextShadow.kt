package de.mm20.launcher2.ui.launcher.widgets.clock

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

/**
 * Personal build addition (see MODIFICATIONS.md): text shadow for the clock widget. All clock
 * styles and clock parts derive their text styles from [Typography], so providing a shadowed
 * typography to the clock widget adds the shadow everywhere without touching each clock.
 *
 * @param darkContent whether the clock draws dark content (then a light glow is used instead).
 */
internal fun Typography.withClockTextShadow(darkContent: Boolean): Typography {
    val shadow = Shadow(
        color = (if (darkContent) Color.White else Color.Black).copy(alpha = 0.45f),
        offset = Offset(0f, 2f),
        blurRadius = 10f,
    )
    return copy(
        displayLarge = displayLarge.copy(shadow = shadow),
        displayMedium = displayMedium.copy(shadow = shadow),
        displaySmall = displaySmall.copy(shadow = shadow),
        headlineLarge = headlineLarge.copy(shadow = shadow),
        headlineMedium = headlineMedium.copy(shadow = shadow),
        headlineSmall = headlineSmall.copy(shadow = shadow),
        titleLarge = titleLarge.copy(shadow = shadow),
        titleMedium = titleMedium.copy(shadow = shadow),
        titleSmall = titleSmall.copy(shadow = shadow),
        bodyLarge = bodyLarge.copy(shadow = shadow),
        bodyMedium = bodyMedium.copy(shadow = shadow),
        bodySmall = bodySmall.copy(shadow = shadow),
        labelLarge = labelLarge.copy(shadow = shadow),
        labelMedium = labelMedium.copy(shadow = shadow),
        labelSmall = labelSmall.copy(shadow = shadow),
    )
}
