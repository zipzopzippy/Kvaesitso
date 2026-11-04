package de.mm20.launcher2.preferences

import kotlinx.serialization.Serializable

/**
 * Personal build addition (see MODIFICATIONS.md): background behind the weather line below the
 * clock, for contrast on busy wallpapers.
 */
@Serializable
enum class ClockWidgetWeatherPill {
    None,
    Light,
    Dark,
}
