package de.mm20.launcher2.preferences.ui

import de.mm20.launcher2.preferences.LauncherDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class IconSettingsData(
    val themedIcons: Boolean,
    /**
     * If true, themed icons are only used on the home screen (dock, home widgets), not in the app drawer.
     */
    val themedIconsHomeOnly: Boolean,
    val forceThemed: Boolean,
    val adaptify: Boolean,
    val iconPack: String?,
)

class IconSettings internal constructor(
    private val launcherDataStore: LauncherDataStore,
) : Flow<IconSettingsData> by (
        launcherDataStore.data.map {
            IconSettingsData(
                themedIcons = it.iconsThemed,
                themedIconsHomeOnly = it.iconsThemedHomeOnly,
                forceThemed = it.iconsForceThemed,
                adaptify = it.iconsAdaptify,
                iconPack = it.iconsPack,
            )
        }
        ) {

    fun setAdaptifyLegacyIcons(adaptify: Boolean) {
        launcherDataStore.update {
            it.copy(iconsAdaptify = adaptify)
        }
    }

    fun setThemedIcons(themedIcons: Boolean) {
        launcherDataStore.update {
            it.copy(iconsThemed = themedIcons)
        }
    }

    fun setThemedIconsHomeOnly(homeOnly: Boolean) {
        launcherDataStore.update {
            it.copy(iconsThemedHomeOnly = homeOnly)
        }
    }

    fun setForceThemedIcons(forceThemed: Boolean) {
        launcherDataStore.update {
            it.copy(iconsForceThemed = forceThemed)
        }
    }

    fun setIconPack(iconPack: String?) {
        launcherDataStore.update {
            it.copy(iconsPack = iconPack)
        }
    }

    fun setIconPackThemed(iconPackThemed: Boolean) {
        launcherDataStore.update {
            it.copy(iconsPackThemed = iconPackThemed)
        }
    }


}