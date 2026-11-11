package de.mm20.launcher2.ui.settings.search

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.plugin.PluginType
import de.mm20.launcher2.preferences.SearchResultType
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.DismissableBottomSheet
import de.mm20.launcher2.ui.component.SmallMessage
import de.mm20.launcher2.ui.component.dragndrop.DraggableItem
import de.mm20.launcher2.ui.component.dragndrop.rememberLazyDragAndDropListState
import de.mm20.launcher2.ui.component.preferences.DragAndDropPreferenceScreen
import de.mm20.launcher2.ui.component.preferences.GuardedPreference
import de.mm20.launcher2.ui.component.preferences.ListPreference
import de.mm20.launcher2.ui.component.preferences.Preference
import de.mm20.launcher2.ui.component.preferences.PreferenceCategory
import de.mm20.launcher2.ui.component.preferences.PreferenceWithSwitch
import de.mm20.launcher2.ui.component.preferences.SwitchPreference
import de.mm20.launcher2.ui.ktx.animateShapeAsState
import de.mm20.launcher2.ui.launcher.search.filters.SearchFilters
import de.mm20.launcher2.ui.locals.LocalBackStack
import de.mm20.launcher2.ui.settings.apps.AppSearchSettingsRoute
import de.mm20.launcher2.ui.settings.appshortcuts.AppShortcutsSettingsRoute
import de.mm20.launcher2.ui.settings.calendarsearch.CalendarProviderSettingsRoute
import de.mm20.launcher2.ui.settings.calendarsearch.CalendarSearchSettingsRoute
import de.mm20.launcher2.ui.settings.contacts.ContactsSettingsRoute
import de.mm20.launcher2.ui.settings.favorites.FavoritesSettingsRoute
import de.mm20.launcher2.ui.settings.filesearch.FileSearchSettingsRoute
import de.mm20.launcher2.ui.settings.filterbar.FilterBarSettingsRoute
import de.mm20.launcher2.ui.settings.hiddenitems.HiddenItemsSettingsRoute
import de.mm20.launcher2.ui.settings.locations.LocationsSettingsRoute
import de.mm20.launcher2.ui.settings.searchactions.SearchActionsSettingsRoute
import de.mm20.launcher2.ui.settings.tags.TagsSettingsRoute
import de.mm20.launcher2.ui.settings.unitconverter.UnitConverterSettingsRoute
import de.mm20.launcher2.ui.settings.wikipedia.WikipediaSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchSettingsRoute : NavKey

@Composable
fun SearchSettingsScreen() {

    val viewModel: SearchSettingsScreenVM = viewModel()
    val context = LocalContext.current

    val backStack = LocalBackStack.current

    var showFilterEditor by remember { mutableStateOf(false) }

    val plugins by viewModel.plugins.collectAsStateWithLifecycle(null)
    val hasCalendarPlugins by remember { derivedStateOf { plugins?.any { it.plugin.type == PluginType.Calendar } } }
    val hasLocationPlugins by remember { derivedStateOf { plugins?.any { it.plugin.type == PluginType.LocationSearch } } }
    val hasContactPlugins by remember { derivedStateOf { plugins?.any { it.plugin.type == PluginType.ContactSearch } } }
    val isTasksAppInstalled by viewModel.isTasksAppInstalled.collectAsStateWithLifecycle()

    val hasAppShortcutsPermission by viewModel.hasAppShortcutPermission.collectAsStateWithLifecycle(
        null
    )
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle(null)
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsStateWithLifecycle(null)
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle(null)

    val favorites by viewModel.favorites.collectAsStateWithLifecycle(null)
    val allApps by viewModel.allApps.collectAsStateWithLifecycle(null)
    val appShortcuts by viewModel.appShortcuts.collectAsStateWithLifecycle(null)
    val calendar by viewModel.calendarSearch.collectAsStateWithLifecycle(null)
    val places by viewModel.placesSearch.collectAsStateWithLifecycle(null)
    val contacts by viewModel.contacts.collectAsStateWithLifecycle(null)
    val calculator by viewModel.calculator.collectAsStateWithLifecycle(null)
    val unitConverter by viewModel.unitConverter.collectAsStateWithLifecycle(null)
    val wikipedia by viewModel.wikipedia.collectAsStateWithLifecycle(null)
    val websites by viewModel.websites.collectAsStateWithLifecycle(null)
    val savedResultOrder by viewModel.resultOrder.collectAsStateWithLifecycle()
    // Reordered locally while dragging and saved on drop, so rows move without waiting for storage
    val resultOrder = remember { mutableStateListOf<SearchResultType>() }
    LaunchedEffect(savedResultOrder) {
        val saved = savedResultOrder ?: return@LaunchedEffect
        if (resultOrder != saved) {
            resultOrder.clear()
            resultOrder.addAll(saved)
        }
    }


    val autoFocus by viewModel.autoFocus.collectAsStateWithLifecycle(null)
    val launchOnEnter by viewModel.launchOnEnter.collectAsStateWithLifecycle(null)
    val reverseSearchResults by viewModel.reverseSearchResults.collectAsStateWithLifecycle(null)
    val filterBar by viewModel.filterBar.collectAsStateWithLifecycle(null)

    val listState = rememberLazyDragAndDropListState(
        onDragStart = {
            it.key is SearchResultType
        },
        onDragEnd = {
            viewModel.setResultOrder(resultOrder.toList())
        },
        onDragCancel = {
            viewModel.setResultOrder(resultOrder.toList())
        },
        onItemMove = { from, to ->
            val fromIndex = resultOrder.indexOf(from.key)
            val toIndex = resultOrder.indexOf(to.key)
            if (fromIndex == -1 || toIndex == -1) return@rememberLazyDragAndDropListState
            resultOrder.add(toIndex, resultOrder.removeAt(fromIndex))
        }
    )

    DragAndDropPreferenceScreen(
        title = {
            Text(
                stringResource(R.string.preference_screen_search),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = 1
            )
        },
        lazyColumnState = listState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            PreferenceCategory {
                PreferenceWithSwitch(
                    title = stringResource(R.string.preference_search_favorites),
                    summary = stringResource(R.string.preference_search_favorites_summary),
                    icon = R.drawable.star_24px,
                    switchValue = favorites == true,
                    onSwitchChanged = {
                        viewModel.setFavorites(it)
                    },
                    onClick = {
                        backStack.add(FavoritesSettingsRoute)
                    }
                )
                PreferenceWithSwitch(
                    title = stringResource(R.string.preference_search_apps),
                    summary = stringResource(R.string.preference_search_apps_summary),
                    icon = R.drawable.apps_24px,
                    switchValue = allApps == true,
                    onSwitchChanged = {
                        viewModel.setAllApps(it)
                    },
                    onClick = {
                        backStack.add(AppSearchSettingsRoute)
                    }
                )
            }
        }
        item(key = "result-order-hint") {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.info_20px), null,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.hint_search_result_order),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        itemsIndexed(
            items = resultOrder,
            key = { _, it -> it }
        ) { index, type ->
            DraggableItem(state = listState, key = type) { dragged ->
                val elevation by animateDpAsState(if (dragged) 4.dp else 0.dp)
                Surface(
                    shadowElevation = elevation,
                    tonalElevation = elevation,
                    modifier = Modifier.zIndex(if (dragged) 1f else 0f),
                    shape = animateShapeAsState(
                        if (dragged) MaterialTheme.shapes.medium
                        else resultRowShape(first = index == 0, last = false)
                    ).value
                ) {
                    when (type) {
                        SearchResultType.Files -> {
                            Preference(
                                title = stringResource(R.string.preference_search_files),
                                summary = stringResource(R.string.preference_search_files_summary),
                                icon = R.drawable.description_24px,
                                onClick = {
                                    backStack.add(FileSearchSettingsRoute)
                                }
                            )
                        }

                        SearchResultType.Contacts -> {
                            if (hasContactPlugins != false) {
                                Preference(
                                    title = stringResource(R.string.preference_search_contacts),
                                    summary = stringResource(R.string.preference_search_contacts_summary),
                                    icon = R.drawable.person_24px,
                                    onClick = {
                                        backStack.add(ContactsSettingsRoute)
                                    },
                                )
                            } else {
                                GuardedPreference(
                                    locked = hasContactsPermission == false,
                                    onUnlock = {
                                        viewModel.requestContactsPermission(context as AppCompatActivity)
                                    },
                                    description = stringResource(R.string.missing_permission_contact_search_settings),
                                ) {
                                    PreferenceWithSwitch(
                                        title = stringResource(R.string.preference_search_contacts),
                                        summary = stringResource(R.string.preference_search_contacts_summary),
                                        icon = R.drawable.person_24px,
                                        switchValue = contacts == true && hasContactsPermission == true,
                                        onSwitchChanged = {
                                            viewModel.setContacts(it)
                                        },
                                        onClick = {
                                            backStack.add(ContactsSettingsRoute)
                                        },
                                        enabled = hasContactsPermission == true
                                    )
                                }
                            }
                        }

                        SearchResultType.Calendar -> {
                            if (hasCalendarPlugins != false || isTasksAppInstalled != false) {
                                Preference(
                                    title = stringResource(R.string.preference_search_calendar),
                                    summary = stringResource(R.string.preference_search_calendar_summary),
                                    icon = R.drawable.today_24px,
                                    onClick = {
                                        backStack.add(CalendarSearchSettingsRoute)
                                    },
                                )
                            } else {

                                GuardedPreference(
                                    locked = hasCalendarPermission == false,
                                    onUnlock = {
                                        viewModel.requestCalendarPermission(context as AppCompatActivity)
                                    },
                                    description = stringResource(R.string.missing_permission_calendar_search_settings),
                                ) {
                                    PreferenceWithSwitch(
                                        title = stringResource(R.string.preference_search_calendar),
                                        summary = stringResource(R.string.preference_search_calendar_summary),
                                        switchValue = calendar == true,
                                        onSwitchChanged = {
                                            viewModel.setCalendarSearch(it)
                                        },
                                        icon = R.drawable.today_24px,
                                        enabled = hasCalendarPermission == true,
                                        onClick = {
                                            backStack.add(CalendarProviderSettingsRoute(providerId = "local"))
                                        }
                                    )
                                }
                            }
                        }

                        SearchResultType.Shortcuts -> {
                            GuardedPreference(
                                locked = hasAppShortcutsPermission == false,
                                onUnlock = {
                                    viewModel.requestAppShortcutsPermission(context as AppCompatActivity)
                                },
                                description = stringResource(
                                    R.string.missing_permission_appshortcuts_search_settings,
                                    stringResource(R.string.app_name),
                                ),
                            ) {
                                PreferenceWithSwitch(
                                    title = stringResource(R.string.preference_search_appshortcuts),
                                    summary = stringResource(R.string.preference_search_appshortcuts_summary),
                                    icon = R.drawable.mobile_arrow_up_right_24px,
                                    switchValue = appShortcuts == true && hasAppShortcutsPermission == true,
                                    onSwitchChanged = {
                                        viewModel.setAppShortcuts(it)
                                    },
                                    enabled = hasAppShortcutsPermission == true,
                                    onClick = {
                                        backStack += AppShortcutsSettingsRoute
                                    }
                                )
                            }
                        }

                        SearchResultType.Calculator -> {
                            SwitchPreference(
                                title = stringResource(R.string.preference_search_calculator),
                                summary = stringResource(R.string.preference_search_calculator_summary),
                                icon = R.drawable.calculate_24px,
                                value = calculator == true,
                                onValueChanged = {
                                    viewModel.setCalculator(it)
                                }
                            )
                        }

                        SearchResultType.UnitConverter -> {
                            PreferenceWithSwitch(
                                title = stringResource(R.string.preference_search_unitconverter),
                                summary = stringResource(R.string.preference_search_unitconverter_summary),
                                icon = R.drawable.autorenew_24px,
                                switchValue = unitConverter == true,
                                onSwitchChanged = {
                                    viewModel.setUnitConverter(it)
                                },
                                onClick = {
                                    backStack.add(UnitConverterSettingsRoute)
                                }
                            )
                        }

                        SearchResultType.Articles -> {
                            PreferenceWithSwitch(
                                title = stringResource(R.string.preference_search_wikipedia),
                                summary = stringResource(R.string.preference_search_wikipedia_summary),
                                icon = R.drawable.wikipedia,
                                switchValue = wikipedia == true,
                                onSwitchChanged = {
                                    viewModel.setWikipedia(it)
                                },
                                onClick = {
                                    backStack.add(WikipediaSettingsRoute)
                                }
                            )
                        }

                        SearchResultType.Websites -> {
                            SwitchPreference(
                                title = stringResource(R.string.preference_search_websites),
                                summary = stringResource(R.string.preference_search_websites_summary),
                                icon = R.drawable.public_24px,
                                value = websites == true,
                                onValueChanged = {
                                    viewModel.setWebsites(it)
                                }
                            )
                        }

                        SearchResultType.Locations -> {
                            GuardedPreference(
                                locked = hasLocationPermission == false,
                                onUnlock = {
                                    viewModel.requestLocationPermission(context as AppCompatActivity)
                                },
                                description = stringResource(R.string.missing_permission_location_search),
                            ) {
                                if (hasLocationPlugins != false) {
                                    Preference(
                                        title = stringResource(R.string.preference_search_locations),
                                        summary = stringResource(R.string.preference_search_locations_summary),
                                        icon = R.drawable.location_on_24px,
                                        enabled = hasLocationPermission == true,
                                        onClick = {
                                            backStack.add(LocationsSettingsRoute)
                                        }
                                    )
                                } else {
                                    PreferenceWithSwitch(
                                        title = stringResource(R.string.preference_search_locations),
                                        summary = stringResource(R.string.preference_search_locations_summary),
                                        icon = R.drawable.location_on_24px,
                                        onClick = {
                                            backStack.add(LocationsSettingsRoute)
                                        },
                                        switchValue = places == true,
                                        onSwitchChanged = {
                                            viewModel.setPlacesSearch(it)
                                        },
                                        enabled = hasLocationPermission == true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "search-actions") {
            Box(modifier = Modifier.clip(resultRowShape(first = false, last = true))) {
                Preference(
                    title = stringResource(R.string.preference_screen_search_actions),
                    summary = stringResource(R.string.preference_search_search_actions_summary),
                    icon = R.drawable.arrow_outward_24px,
                    onClick = {
                        backStack.add(SearchActionsSettingsRoute)
                    }
                )
            }
        }
        item {
            Box(modifier = Modifier.padding(top = 10.dp)) {
                PreferenceCategory {
                    Preference(
                        title = stringResource(R.string.preference_hidden_items),
                        summary = stringResource(R.string.preference_hidden_items_summary),
                        icon = R.drawable.visibility_off_24px,
                        onClick = {
                            backStack.add(HiddenItemsSettingsRoute)
                        }
                    )
                    Preference(
                        title = stringResource(R.string.preference_screen_tags),
                        summary = stringResource(R.string.preference_screen_tags_summary),
                        icon = R.drawable.tag_24px,
                        onClick = {
                            backStack.add(TagsSettingsRoute)
                        }
                    )
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(top = 10.dp)) {
                PreferenceCategory {
                    Preference(
                        title = stringResource(R.string.preference_default_filter),
                        summary = stringResource(R.string.preference_default_filter_summary),
                        icon = R.drawable.filter_alt_24px,
                        onClick = {
                            showFilterEditor = true
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.preference_filter_bar),
                        iconPadding = true,
                        summary = stringResource(R.string.preference_filter_bar_summary),
                        value = filterBar == true,
                        onValueChanged = {
                            viewModel.setFilterBar(it)
                        }
                    )
                    AnimatedVisibility(filterBar == true) {
                        Preference(
                            title = stringResource(R.string.preference_customize_filter_bar),
                            iconPadding = true,
                            summary = stringResource(R.string.preference_customize_filter_bar_summary),
                            onClick = {
                                backStack.add(FilterBarSettingsRoute)
                            }
                        )
                    }
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(top = 10.dp)) {
                PreferenceCategory {
                    SwitchPreference(
                        title = stringResource(R.string.preference_search_bar_auto_focus),
                        summary = stringResource(R.string.preference_search_bar_auto_focus_summary),
                        icon = R.drawable.keyboard_24px,
                        value = autoFocus == true,
                        onValueChanged = {
                            viewModel.setAutoFocus(it)
                        }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.preference_search_bar_launch_on_enter),
                        iconPadding = true,
                        summary = stringResource(R.string.preference_search_bar_launch_on_enter_summary),
                        value = launchOnEnter == true,
                        onValueChanged = {
                            viewModel.setLaunchOnEnter(it)
                        }
                    )
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(top = 10.dp)) {
                PreferenceCategory {
                    ListPreference(
                        title = stringResource(R.string.preference_layout_search_results),
                        items = listOf(
                            stringResource(R.string.search_results_order_top_down) to false,
                            stringResource(R.string.search_results_order_bottom_up) to true,
                        ),
                        value = reverseSearchResults,
                        onValueChanged = {
                            if (it != null) viewModel.setReverseSearchResults(it)
                        },
                        icon = R.drawable.sort_24px
                    )
                }
            }
        }
    }

    DismissableBottomSheet(
        expanded = showFilterEditor,
        onDismissRequest = { showFilterEditor = false }) {
        val filters by viewModel.searchFilters.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            AnimatedVisibility(filters.allowNetwork) {
                SmallMessage(
                    modifier = Modifier.padding(bottom = 16.dp),
                    icon = R.drawable.warning_24px,
                    text = stringResource(R.string.filter_settings_network_warning)
                )
            }
            SearchFilters(
                filters = filters,
                onFiltersChange = {
                    viewModel.setSearchFilters(it)
                },
                settings = true,
            )
        }
    }
}

/**
 * Shape of a row in the reorderable search results section, so the rows look like one
 * [PreferenceCategory]: rounded at the top of the first row and the bottom of the last.
 */
@Composable
private fun resultRowShape(first: Boolean, last: Boolean): Shape {
    val shape = MaterialTheme.shapes.medium
    val none = CornerSize(0.dp)
    return shape.copy(
        topStart = if (first) shape.topStart else none,
        topEnd = if (first) shape.topEnd else none,
        bottomStart = if (last) shape.bottomStart else none,
        bottomEnd = if (last) shape.bottomEnd else none,
    )
}
