package de.mm20.launcher2.weather.breezy

import kotlinx.serialization.Serializable

/**
 * The parts of Breezy Weather's data sharing format that the launcher uses, see
 * https://github.com/breezy-weather/breezy-weather-data-sharing-lib
 */
@Serializable
internal data class BreezyWeatherData(
    val refreshTime: Long? = null,
    val current: Current? = null,
    val daily: List<Daily>? = null,
    val hourly: List<Hourly>? = null,
) {
    @Serializable
    data class Current(
        val weatherCode: String? = null,
        val temperature: Temperature? = null,
    )

    @Serializable
    data class Daily(
        val date: Long,
        val day: HalfDay? = null,
        val night: HalfDay? = null,
    )

    @Serializable
    data class HalfDay(
        val weatherCode: String? = null,
        val temperature: Temperature? = null,
    )

    @Serializable
    data class Hourly(
        val date: Long,
        val isDaylight: Boolean = true,
        val weatherText: String? = null,
        val weatherCode: String? = null,
        val temperature: Temperature? = null,
        val precipitation: Precipitation? = null,
        val precipitationProbability: Precipitation? = null,
        val wind: Wind? = null,
        val uV: Value? = null,
        val relativeHumidity: Value? = null,
        val pressure: Value? = null,
        val cloudCover: Value? = null,
    )

    @Serializable
    data class Temperature(val temperature: Value? = null)

    @Serializable
    data class Precipitation(val total: Value? = null, val snow: Value? = null)

    @Serializable
    data class Wind(val degree: Double? = null, val speed: Value? = null)

    @Serializable
    data class Value(val value: Double? = null, val unit: String? = null)
}
