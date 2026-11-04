package de.mm20.launcher2.currencies

import android.util.Log
import de.mm20.launcher2.crashreporter.CrashReporter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Personal build addition (see MODIFICATIONS.md): the ECB only publishes ~30 currencies, so fetch
 * EUR based rates for the remaining ones (e.g. TWD) from open.er-api.com (free, no API key,
 * updated daily). Currencies in [exclude] (the ECB's own) are never overwritten.
 *
 * Returns an empty list if the source is unavailable, so that it can't fail the ECB update.
 */
internal suspend fun fetchSupplementaryExchangeRates(
    httpClient: HttpClient,
    exclude: Set<String>,
): List<Pair<String, Double>> {
    return try {
        val body = httpClient.get { url("https://open.er-api.com/v6/latest/EUR") }.bodyAsText()
        val json = JSONObject(body)
        if (json.optString("result") != "success" || json.optString("base_code") != "EUR") {
            return emptyList()
        }
        val rates = json.getJSONObject("rates")
        rates.keys().asSequence()
            .filter { it !in exclude && it.isIsoCurrency() }
            .mapNotNull { code -> rates.optDouble(code).takeIf { it.isFinite() && it > 0.0 }?.let { code to it } }
            .toList()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("MM20", "Could not fetch supplementary exchange rates", e)
        CrashReporter.logException(e)
        emptyList()
    }
}

private fun String.isIsoCurrency(): Boolean =
    try {
        java.util.Currency.getInstance(this) != null
    } catch (e: IllegalArgumentException) {
        false
    }
