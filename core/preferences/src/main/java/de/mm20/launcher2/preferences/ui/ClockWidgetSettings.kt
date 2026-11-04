package de.mm20.launcher2.preferences.ui

import de.mm20.launcher2.preferences.BatteryStatusVisibility
import de.mm20.launcher2.preferences.ClockWidgetAlignment
import de.mm20.launcher2.preferences.ClockWidgetColors
import de.mm20.launcher2.preferences.ClockWidgetStyle
import de.mm20.launcher2.preferences.ClockWidgetStyleEnum
import de.mm20.launcher2.preferences.LauncherDataStore
import de.mm20.launcher2.preferences.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class ClockWidgetParts(
    val date: Boolean,
    val music: Boolean = false,
    val battery: BatteryStatusVisibility = BatteryStatusVisibility.Hide,
    val alarm: Boolean = false,
)

class ClockWidgetSettings internal constructor(
    private val launcherDataStore: LauncherDataStore,
) {
    val compact
        get() = launcherDataStore.data.map { it.clockWidgetCompact }

    fun setCompact(compact: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetCompact = compact)
        }
    }

    val parts
        get() = launcherDataStore.data.map {
            ClockWidgetParts(
                date = it.clockWidgetDatePart,
                music = it.clockWidgetMusicPart,
                battery = it.clockWidgetBatteryPart,
                alarm = it.clockWidgetAlarmPart,
            )
        }.distinctUntilChanged()

    fun setDatePart(datePart: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetDatePart = datePart)
        }
    }

    fun setMusicPart(musicPart: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetMusicPart = musicPart)
        }
    }

    fun setBatteryPart(batteryPart: BatteryStatusVisibility) {
        launcherDataStore.update {
            it.copy(clockWidgetBatteryPart = batteryPart)
        }
    }

    fun setAlarmPart(alarmPart: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetAlarmPart = alarmPart)
        }
    }

    val fillHeight
        get() = launcherDataStore.data.map { it.clockWidgetFillHeight || !it.homeScreenWidgets }

    fun setFillHeight(fillHeight: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetFillHeight = fillHeight)
        }
    }

    val dock
        get() = launcherDataStore.data.map { it.homeScreenDock }

    /**
     * True if the dock is enabled and should be pinned just above the search bar
     * (instead of in the middle of the screen, below the clock).
     */
    val dockAboveSearchBar
        get() = launcherDataStore.data.map { it.homeScreenDock && it.homeScreenDockAboveSearchBar }

    val alignment
        get() = launcherDataStore.data.map { it.clockWidgetAlignment }

    fun setAlignment(alignment: ClockWidgetAlignment) {
        launcherDataStore.update {
            it.copy(clockWidgetAlignment = alignment)
        }
    }

    val clockStyle: Flow<ClockWidgetStyle>
        get() = launcherDataStore.data.map {
            when (it.clockWidgetStyle) {
                ClockWidgetStyleEnum.Digital1 -> it.clockWidgetDigital1
                ClockWidgetStyleEnum.Digital2 -> ClockWidgetStyle.Digital2
                ClockWidgetStyleEnum.Orbit -> ClockWidgetStyle.Orbit
                ClockWidgetStyleEnum.Analog -> it.clockWidgetAnalog
                ClockWidgetStyleEnum.Binary -> ClockWidgetStyle.Binary
                ClockWidgetStyleEnum.Segment -> ClockWidgetStyle.Segment
                ClockWidgetStyleEnum.Empty -> ClockWidgetStyle.Empty
                ClockWidgetStyleEnum.Custom -> it.clockWidgetCustom
            }
        }

    val digital1: Flow<ClockWidgetStyle.Digital1>
        get() = launcherDataStore.data.map { it.clockWidgetDigital1 }

    val analog: Flow<ClockWidgetStyle.Analog>
        get() = launcherDataStore.data.map { it.clockWidgetAnalog }

    val custom: Flow<ClockWidgetStyle.Custom>
        get() = launcherDataStore.data.map { it.clockWidgetCustom }

    fun setClockStyle(clockStyle: ClockWidgetStyle) {
        launcherDataStore.update {
            it.copy(
                clockWidgetStyle = clockStyle.enumValue,
                clockWidgetDigital1 = clockStyle as? ClockWidgetStyle.Digital1 ?: it.clockWidgetDigital1,
                clockWidgetAnalog = clockStyle as? ClockWidgetStyle.Analog ?: it.clockWidgetAnalog,
                clockWidgetCustom = clockStyle as? ClockWidgetStyle.Custom ?: it.clockWidgetCustom,
            )
        }
    }

    val color
        get() = launcherDataStore.data.map { it.clockWidgetColors }

    fun setColor(color: ClockWidgetColors) {
        launcherDataStore.update {
            it.copy(clockWidgetColors = color)
        }
    }

    val showSeconds
        get() = launcherDataStore.data.map { it.clockWidgetShowSeconds }

    fun setShowSeconds(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetShowSeconds = enabled)
        }
    }

    val monospaced
        get() = launcherDataStore.data.map { it.clockWidgetMonospaced }

    fun setMonospaced(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetMonospaced = enabled)
        }
    }

    val useThemeColor
        get() = launcherDataStore.data.map { it.clockWidgetUseThemeColor }

    fun setUseThemeColor(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetUseThemeColor = enabled)
        }
    }

    // Personal build (see MODIFICATIONS.md)
    val textShadow
        get() = launcherDataStore.data.map { it.clockWidgetTextShadow }

    fun setTextShadow(enabled: Boolean) {
        launcherDataStore.update { it.copy(clockWidgetTextShadow = enabled) }
    }

    val weather
        get() = launcherDataStore.data.map { it.clockWidgetWeather }

    fun setWeather(enabled: Boolean) {
        launcherDataStore.update { it.copy(clockWidgetWeather = enabled) }
    }

    val weatherPrecipitation
        get() = launcherDataStore.data.map { it.clockWidgetWeatherPrecipitation }

    fun setWeatherPrecipitation(enabled: Boolean) {
        launcherDataStore.update { it.copy(clockWidgetWeatherPrecipitation = enabled) }
    }

    val weatherPill
        get() = launcherDataStore.data.map { it.clockWidgetWeatherPill }

    fun setWeatherPill(pill: de.mm20.launcher2.preferences.ClockWidgetWeatherPill) {
        launcherDataStore.update { it.copy(clockWidgetWeatherPill = pill) }
    }

    val useSmartspacer
        get() = launcherDataStore.data.map { it.clockWidgetSmartspacer }

    fun setUseSmartspacer(enabled: Boolean) {
        launcherDataStore.update {
            it.copy(clockWidgetSmartspacer = enabled)
        }
    }

}

internal val ClockWidgetStyle.enumValue
    get() = when (this) {
        is ClockWidgetStyle.Digital1 -> ClockWidgetStyleEnum.Digital1
        is ClockWidgetStyle.Digital2 -> ClockWidgetStyleEnum.Digital2
        is ClockWidgetStyle.Orbit -> ClockWidgetStyleEnum.Orbit
        is ClockWidgetStyle.Analog -> ClockWidgetStyleEnum.Analog
        is ClockWidgetStyle.Binary -> ClockWidgetStyleEnum.Binary
        is ClockWidgetStyle.Segment -> ClockWidgetStyleEnum.Segment
        is ClockWidgetStyle.Empty -> ClockWidgetStyleEnum.Empty
        is ClockWidgetStyle.Custom -> ClockWidgetStyleEnum.Custom
    }
