package de.mm20.launcher2.weather

import de.mm20.launcher2.preferences.weather.WeatherLocation
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

interface WeatherProvider {

    /** Whether the provider picks the location itself, so that no location setting is needed. */
    val managesLocation: Boolean
        get() = false

    suspend fun getUpdateInterval(): Long {
        return 1000 * 60 * 60L
    }

    /** The provider's own summary, if it offers one. */
    suspend fun getSummary(): WeatherSummary? = null

    suspend fun getWeatherData(location: WeatherLocation): List<Forecast>?
    suspend fun getWeatherData(lat: Double, lon: Double): List<Forecast>?
    suspend fun findLocation(query: String): List<WeatherLocation>

    companion object: KoinComponent {
        internal fun getInstance(providerId: String): WeatherProvider {
            return get { parametersOf(providerId) }
        }
    }
}