package de.mm20.launcher2.weather.breezy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.mm20.launcher2.weather.WeatherRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Breezy Weather's update notifier: tells us that new data can be read from its content provider.
 */
class BreezyWeatherReceiver : BroadcastReceiver(), KoinComponent {

    private val repository: WeatherRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        repository.onProviderDataChanged(BreezyWeatherProvider.Id)
    }
}
