package de.mm20.launcher2.weather.breezy

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import de.mm20.launcher2.crashreporter.CrashReporter
import de.mm20.launcher2.preferences.weather.WeatherLocation
import de.mm20.launcher2.serialization.Json
import de.mm20.launcher2.weather.Forecast
import de.mm20.launcher2.weather.R
import de.mm20.launcher2.weather.WeatherIcon
import de.mm20.launcher2.weather.WeatherProvider
import de.mm20.launcher2.weather.WeatherSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream

/**
 * Reads forecasts from Breezy Weather's content provider. Breezy announces new data to
 * [BreezyWeatherReceiver], the regular update interval is only a fallback.
 */
class BreezyWeatherProvider(
    private val context: Context,
) : WeatherProvider {

    override val managesLocation = true

    override suspend fun getWeatherData(location: WeatherLocation) = loadForecasts()

    override suspend fun getWeatherData(lat: Double, lon: Double) = loadForecasts()

    override suspend fun findLocation(query: String): List<WeatherLocation> = emptyList()

    override suspend fun getSummary(): WeatherSummary? = readBreezy {
        val location = queryFirstLocation() ?: return@readBreezy null
        val weather = queryWeather(location.id, hourly = false, daily = true)
            ?: return@readBreezy null
        WeatherSummary(
            current = weather.current?.let { period(it.weatherCode, it.temperature) },
            days = weather.daily.orEmpty().map { daily ->
                WeatherSummary.Day(
                    date = daily.date,
                    day = daily.day?.let { period(it.weatherCode, it.temperature) },
                    night = daily.night?.let { period(it.weatherCode, it.temperature) },
                )
            },
        )
    }

    private fun period(
        code: String?,
        temperature: BreezyWeatherData.Temperature?,
    ): WeatherSummary.Period? {
        return WeatherSummary.Period(
            icon = iconForCode(code).id,
            temperature = temperature?.temperature?.valueIn(UnitKelvin) ?: return null,
        )
    }

    private suspend fun loadForecasts(): List<Forecast>? = readBreezy {
        val location = queryFirstLocation() ?: return@readBreezy null
        val weather = queryWeather(location.id, hourly = true, daily = false)
            ?: return@readBreezy null
        val updateTime = weather.refreshTime ?: System.currentTimeMillis()
        // An empty list would wipe the stored forecasts, keep them instead.
        weather.hourly.orEmpty()
            .mapNotNull { it.toForecast(location.name, updateTime) }
            .takeIf { it.isNotEmpty() }
    }

    private suspend fun <T> readBreezy(read: () -> T?): T? = withContext(Dispatchers.IO) {
        try {
            read()
        } catch (e: SecurityException) {
            Log.w("BreezyWeatherProvider", "Missing permission to read Breezy Weather data", e)
            null
        } catch (e: Exception) {
            // Another app's provider and data format: a surprise here must not crash the home screen.
            CrashReporter.logException(e)
            null
        }
    }

    private fun queryFirstLocation(): BreezyLocation? {
        val uri = baseUri.buildUpon().appendPath("locations").appendQueryParameter("limit", "1").build()
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            BreezyLocation(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("custom_name"))
                    ?: cursor.getString(cursor.getColumnIndexOrThrow("city")),
            )
        }
    }

    private fun queryWeather(locationId: String, hourly: Boolean, daily: Boolean): BreezyWeatherData? {
        val uri = baseUri.buildUpon().appendPath("weather")
            .appendQueryParameter("withHourly", hourly.toString())
            .appendQueryParameter("withDaily", daily.toString())
            .appendQueryParameter("withMinutely", "false")
            .appendQueryParameter("withAlerts", "false")
            .appendQueryParameter("withNormals", "false")
            .appendQueryParameter("temperatureUnit", UnitKelvin)
            .appendQueryParameter("precipitationUnit", UnitMillimeters)
            .appendQueryParameter("speedUnit", UnitMetersPerSecond)
            .appendQueryParameter("pressureUnit", UnitHectopascals)
            .build()
        return context.contentResolver.query(uri, null, "id=$locationId", null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val blob = cursor.getBlob(cursor.getColumnIndexOrThrow("weather")) ?: return null
            val json = GZIPInputStream(blob.inputStream()).bufferedReader().use { it.readText() }
            Json.Lenient.decodeFromString<BreezyWeatherData>(json)
        }
    }

    private fun BreezyWeatherData.Hourly.toForecast(location: String, updateTime: Long): Forecast? {
        return Forecast(
            timestamp = date,
            temperature = temperature?.temperature?.valueIn(UnitKelvin) ?: return null,
            icon = iconForCode(weatherCode).id,
            condition = weatherText.orEmpty(),
            night = !isDaylight,
            location = location,
            provider = context.getString(R.string.provider_breezy),
            precipitation = precipitation?.total?.valueIn(UnitMillimeters),
            snow = precipitation?.snow?.valueIn(UnitMillimeters),
            precipProbability = precipitationProbability?.total?.value?.toInt(),
            windSpeed = wind?.speed?.valueIn(UnitMetersPerSecond),
            windDirection = wind?.degree?.takeIf { it >= 0 },
            pressure = pressure?.valueIn(UnitHectopascals),
            humidity = relativeHumidity?.value,
            clouds = cloudCover?.value?.toInt(),
            uvIndex = uV?.value,
            updateTime = updateTime,
        )
    }

    /** Breezy may return another unit than the requested one if it stops supporting it. */
    private fun BreezyWeatherData.Value.valueIn(requestedUnit: String): Double? {
        return value?.takeIf { unit == requestedUnit }
    }

    private fun iconForCode(code: String?): WeatherIcon {
        return when (code) {
            "clear" -> WeatherIcon.Clear
            "partly_cloudy" -> WeatherIcon.PartlyCloudy
            "cloudy" -> WeatherIcon.Overcast
            "rain" -> WeatherIcon.Rain
            "snow" -> WeatherIcon.Snow
            "wind" -> WeatherIcon.Wind
            "fog" -> WeatherIcon.Fog
            "haze" -> WeatherIcon.Haze
            "sleet" -> WeatherIcon.Sleet
            "hail" -> WeatherIcon.Hail
            "thunder" -> WeatherIcon.Thunder
            "thunderstorm" -> WeatherIcon.Thunderstorm
            else -> WeatherIcon.Unknown
        }
    }

    private data class BreezyLocation(val id: String, val name: String)

    companion object {
        internal fun isAvailable(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo("org.breezyweather", 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        const val Id = "breezy"

        private val baseUri = Uri.parse("content://org.breezyweather.provider.weather")
        private const val UnitKelvin = "k"
        private const val UnitMillimeters = "mm"
        private const val UnitMetersPerSecond = "mps"
        private const val UnitHectopascals = "hpa"
    }
}
