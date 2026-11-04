package de.mm20.launcher2.appshortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.StringRes
import de.mm20.launcher2.icons.ColorLayer
import de.mm20.launcher2.icons.LauncherIcon
import de.mm20.launcher2.icons.StaticIconLayer
import de.mm20.launcher2.icons.StaticLauncherIcon
import de.mm20.launcher2.icons.TintedIconLayer
import de.mm20.launcher2.icons.TransparentLayer
import de.mm20.launcher2.ktx.isAtLeastApiLevel
import de.mm20.launcher2.ktx.jsonObjectOf
import de.mm20.launcher2.ktx.tryStartActivity
import de.mm20.launcher2.search.AppShortcut
import de.mm20.launcher2.search.ResultScore
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.search.SearchableDeserializer
import de.mm20.launcher2.search.SearchableSerializer
import de.mm20.launcher2.search.StringNormalizer
import org.json.JSONObject

/**
 * A page of the system settings app (Wi-Fi, Bluetooth, Display, ...), shown in shortcut search
 * results. Personal build addition, see MODIFICATIONS.md.
 */
internal data class DeviceSettingsShortcut(
    val action: String,
    override val label: String,
    override val appName: String?,
    override val packageName: String?,
    override val labelOverride: String? = null,
    override val score: ResultScore = ResultScore.Unspecified,
) : AppShortcut {

    override val domain = Domain
    override val key: String = "$domain://$action"

    override val componentName: ComponentName?
        get() = null

    override fun overrideLabel(label: String): DeviceSettingsShortcut {
        return this.copy(labelOverride = label)
    }

    override fun launch(context: Context, options: Bundle?): Boolean {
        return context.tryStartActivity(
            Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            options,
        )
    }

    override suspend fun loadIcon(context: Context, size: Int, themed: Boolean): LauncherIcon? {
        val pkg = packageName ?: return null
        val icon = try {
            context.packageManager.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        if (icon is AdaptiveIconDrawable) {
            if (themed && isAtLeastApiLevel(33) && icon.monochrome != null) {
                return StaticLauncherIcon(
                    foregroundLayer = TintedIconLayer(scale = 1f, icon = icon.monochrome!!),
                    backgroundLayer = ColorLayer(),
                )
            }
            return StaticLauncherIcon(
                foregroundLayer = icon.foreground?.let { StaticIconLayer(icon = it, scale = 1.5f) }
                    ?: TransparentLayer,
                backgroundLayer = icon.background?.let { StaticIconLayer(icon = it, scale = 1.5f) }
                    ?: TransparentLayer,
            )
        }
        return StaticLauncherIcon(
            foregroundLayer = StaticIconLayer(icon = icon, scale = 1f),
            backgroundLayer = TransparentLayer,
        )
    }

    override fun getSerializer(): SearchableSerializer = DeviceSettingsShortcutSerializer()

    companion object {
        const val Domain = "devicesettings"
    }
}

private class DeviceSettingsPage(
    val action: String,
    @StringRes val label: Int,
    val keywords: List<String> = emptyList(),
)

/**
 * The searchable system settings pages. Pages whose intent does not resolve on the device are
 * left out.
 */
private val DeviceSettingsPages = listOf(
    DeviceSettingsPage(Settings.ACTION_WIFI_SETTINGS, R.string.device_settings_wifi, listOf("wifi", "wlan", "wireless")),
    DeviceSettingsPage(Settings.ACTION_BLUETOOTH_SETTINGS, R.string.device_settings_bluetooth),
    DeviceSettingsPage(Settings.ACTION_WIRELESS_SETTINGS, R.string.device_settings_network, listOf("internet", "connections")),
    DeviceSettingsPage(Settings.ACTION_NETWORK_OPERATOR_SETTINGS, R.string.device_settings_mobile_network, listOf("sim", "carrier", "mobile data", "cellular")),
    DeviceSettingsPage(Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.string.device_settings_airplane_mode, listOf("flight mode")),
    DeviceSettingsPage("android.settings.DATA_USAGE_SETTINGS", R.string.device_settings_data_usage),
    DeviceSettingsPage(Settings.ACTION_NFC_SETTINGS, R.string.device_settings_nfc, listOf("tap to pay", "contactless")),
    DeviceSettingsPage(Settings.ACTION_VPN_SETTINGS, R.string.device_settings_vpn),
    DeviceSettingsPage(Settings.ACTION_CAST_SETTINGS, R.string.device_settings_cast, listOf("screen mirroring")),
    DeviceSettingsPage(Settings.ACTION_DISPLAY_SETTINGS, R.string.device_settings_display, listOf("screen", "brightness", "dark theme", "font size")),
    DeviceSettingsPage(Settings.ACTION_NIGHT_DISPLAY_SETTINGS, R.string.device_settings_night_light, listOf("blue light")),
    DeviceSettingsPage(Settings.ACTION_DREAM_SETTINGS, R.string.device_settings_screen_saver, listOf("daydream")),
    DeviceSettingsPage(Settings.ACTION_SOUND_SETTINGS, R.string.device_settings_sound, listOf("volume", "ringtone", "vibration")),
    DeviceSettingsPage("android.settings.ZEN_MODE_SETTINGS", R.string.device_settings_do_not_disturb, listOf("dnd", "modes", "silent")),
    DeviceSettingsPage("android.settings.NOTIFICATION_SETTINGS", R.string.device_settings_notifications),
    DeviceSettingsPage(Intent.ACTION_POWER_USAGE_SUMMARY, R.string.device_settings_battery, listOf("battery usage", "power")),
    DeviceSettingsPage(Settings.ACTION_BATTERY_SAVER_SETTINGS, R.string.device_settings_battery_saver, listOf("power saving")),
    DeviceSettingsPage(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, R.string.device_settings_storage, listOf("space", "free up")),
    DeviceSettingsPage(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, R.string.device_settings_apps, listOf("applications")),
    DeviceSettingsPage(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, R.string.device_settings_default_apps, listOf("default browser", "home app")),
    DeviceSettingsPage(Settings.ACTION_SECURITY_SETTINGS, R.string.device_settings_security, listOf("screen lock", "fingerprint", "face unlock", "pin", "password")),
    DeviceSettingsPage(Settings.ACTION_PRIVACY_SETTINGS, R.string.device_settings_privacy, listOf("permissions")),
    DeviceSettingsPage(Settings.ACTION_LOCATION_SOURCE_SETTINGS, R.string.device_settings_location, listOf("gps")),
    DeviceSettingsPage(Settings.ACTION_ACCESSIBILITY_SETTINGS, R.string.device_settings_accessibility),
    DeviceSettingsPage(Settings.ACTION_SYNC_SETTINGS, R.string.device_settings_accounts, listOf("sync", "passwords")),
    DeviceSettingsPage("android.settings.USER_SETTINGS", R.string.device_settings_users, listOf("multiple users", "guest")),
    DeviceSettingsPage(Settings.ACTION_DATE_SETTINGS, R.string.device_settings_date_time, listOf("time zone", "clock")),
    DeviceSettingsPage(Settings.ACTION_LOCALE_SETTINGS, R.string.device_settings_languages, listOf("language", "locale")),
    DeviceSettingsPage(Settings.ACTION_INPUT_METHOD_SETTINGS, R.string.device_settings_keyboard, listOf("input", "on-screen keyboard")),
    DeviceSettingsPage(Settings.ACTION_PRINT_SETTINGS, R.string.device_settings_printing, listOf("printer")),
    DeviceSettingsPage(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, R.string.device_settings_developer_options, listOf("developer", "usb debugging", "adb")),
    DeviceSettingsPage(Settings.ACTION_DEVICE_INFO_SETTINGS, R.string.device_settings_about_phone, listOf("about device", "android version", "build number", "model")),
)

private fun settingsPackage(context: Context): String? =
    context.packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName

private fun settingsAppName(context: Context, packageName: String?): String? = try {
    packageName?.let {
        context.packageManager.getApplicationInfo(it, 0).loadLabel(context.packageManager).toString()
    }
} catch (e: PackageManager.NameNotFoundException) {
    null
}

private fun DeviceSettingsPage.resolves(context: Context): Boolean =
    context.packageManager.resolveActivity(Intent(action), 0) != null

/**
 * Searches the system settings pages. [normalizedQuery] must already be normalized with
 * [stringNormalizer].
 */
internal fun searchDeviceSettings(
    context: Context,
    normalizedQuery: String,
    stringNormalizer: StringNormalizer,
): List<DeviceSettingsShortcut> {
    val pkg = settingsPackage(context) ?: return emptyList()
    val appName = settingsAppName(context, pkg)
    return DeviceSettingsPages.mapNotNull { page ->
        val label = context.getString(page.label)
        val score = ResultScore.from(
            query = normalizedQuery,
            primaryFields = (listOf(label) + page.keywords).map { stringNormalizer.normalize(it) },
        )
        if (score.score < 0.8f || !page.resolves(context)) return@mapNotNull null
        DeviceSettingsShortcut(
            action = page.action,
            label = label,
            appName = appName,
            packageName = pkg,
            score = score,
        )
    }
}

internal class DeviceSettingsShortcutSerializer : SearchableSerializer {
    override fun serialize(searchable: SavableSearchable): String {
        searchable as DeviceSettingsShortcut
        return jsonObjectOf("action" to searchable.action).toString()
    }

    override val typePrefix: String
        get() = DeviceSettingsShortcut.Domain
}

internal class DeviceSettingsShortcutDeserializer(
    private val context: Context,
) : SearchableDeserializer {
    override suspend fun deserialize(serialized: String): SavableSearchable? {
        val action = JSONObject(serialized).getString("action")
        val page = DeviceSettingsPages.firstOrNull { it.action == action } ?: return null
        val pkg = settingsPackage(context) ?: return null
        return DeviceSettingsShortcut(
            action = action,
            label = context.getString(page.label),
            appName = settingsAppName(context, pkg),
            packageName = pkg,
        )
    }
}
