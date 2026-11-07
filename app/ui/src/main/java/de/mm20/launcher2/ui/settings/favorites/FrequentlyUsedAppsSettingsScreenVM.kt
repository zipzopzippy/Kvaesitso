package de.mm20.launcher2.ui.settings.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.badges.Badge
import de.mm20.launcher2.badges.BadgeService
import de.mm20.launcher2.icons.IconService
import de.mm20.launcher2.icons.LauncherIcon
import de.mm20.launcher2.preferences.search.FavoritesSettings
import de.mm20.launcher2.search.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FrequentlyUsedAppsSettingsScreenVM : ViewModel(), KoinComponent {
    private val appRepository: AppRepository by inject()
    private val favoritesSettings: FavoritesSettings by inject()
    private val iconService: IconService by inject()
    private val badgeService: BadgeService by inject()

    val apps = combine(
        appRepository.findMany(),
        favoritesSettings.frequentlyUsedExcluded,
    ) { apps, excluded ->
        apps.distinctBy { it.key }
            .sorted()
            .map { FrequentlyUsedApp(app = it, isChecked = it.key !in excluded) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun onAppChecked(app: FrequentlyUsedApp, checked: Boolean) {
        favoritesSettings.setFrequentlyUsedExcluded(app.app.key, !checked)
    }

    fun getIcon(app: Application, size: Int): Flow<LauncherIcon?> {
        return iconService.getIcon(app, size)
    }

    fun getBadge(app: Application): Flow<Badge?> {
        return badgeService.getBadge(app)
    }
}

data class FrequentlyUsedApp(
    val app: Application,
    val isChecked: Boolean,
)
