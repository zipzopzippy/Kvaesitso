package de.mm20.launcher2.weather

/**
 * The provider's own summary of the weather: its current conditions, and its day and night
 * forecast for each date. Icons are one of the [Forecast] icon constants, temperatures in Kelvin.
 */
data class WeatherSummary(
    val current: Period?,
    val days: List<Day>,
) {
    data class Period(val icon: Int, val temperature: Double)

    data class Day(
        /** Timestamp within the date this forecast is for */
        val date: Long,
        val day: Period?,
        /** The night that starts on the evening of this date */
        val night: Period?,
    )
}
