package de.mm20.launcher2.ui.launcher.search.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import de.mm20.launcher2.preferences.KeyboardFilterBarItem
import de.mm20.launcher2.preferences.search.CalculatorSearchSettings
import de.mm20.launcher2.preferences.search.CalendarSearchSettings
import de.mm20.launcher2.preferences.search.ContactSearchSettings
import de.mm20.launcher2.preferences.search.FileSearchSettings
import de.mm20.launcher2.preferences.search.LocationSearchSettings
import de.mm20.launcher2.preferences.search.ShortcutSearchSettings
import de.mm20.launcher2.preferences.search.UnitConverterSettings
import de.mm20.launcher2.preferences.search.WebsiteSearchSettings
import de.mm20.launcher2.preferences.search.WikipediaSearchSettings
import de.mm20.launcher2.search.SearchFilters
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/**
 * Personal build addition (see MODIFICATIONS.md): which search categories are turned on in the
 * settings, as a [SearchFilters] where true means that its filter chip is shown.
 */
@Composable
internal fun availableSearchFilters(): SearchFilters {
    val files: FileSearchSettings = koinInject()
    val contacts: ContactSearchSettings = koinInject()
    val events: CalendarSearchSettings = koinInject()
    val shortcuts: ShortcutSearchSettings = koinInject()
    val articles: WikipediaSearchSettings = koinInject()
    val websites: WebsiteSearchSettings = koinInject()
    val places: LocationSearchSettings = koinInject()
    val calculator: CalculatorSearchSettings = koinInject()
    val unitConverter: UnitConverterSettings = koinInject()

    val available by remember {
        combine(
            files.enabledProviders.map { it.isNotEmpty() },
            contacts.enabledProviders.map { it.isNotEmpty() },
            events.enabledProviders.map { it.isNotEmpty() },
            shortcuts.enabled,
            articles.enabled,
            websites.enabled,
            places.enabledProviders.map { it.isNotEmpty() },
            combine(calculator.enabled, unitConverter.enabled) { a, b -> a || b },
        ) { enabled ->
            SearchFilters(
                files = enabled[0],
                contacts = enabled[1],
                events = enabled[2],
                shortcuts = enabled[3],
                articles = enabled[4],
                websites = enabled[5],
                places = enabled[6],
                tools = enabled[7],
            )
        }
    }.collectAsState(SearchFilters())
    return available
}

internal fun SearchFilters.isAvailable(item: KeyboardFilterBarItem): Boolean {
    return !item.isCategory || valueOf(item)
}
