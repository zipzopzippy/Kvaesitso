package de.mm20.launcher2.ui.launcher.search.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.search.common.grid.GridItem
import de.mm20.launcher2.ui.launcher.search.common.grid.GridResults
import de.mm20.launcher2.ui.launcher.search.common.list.ListItem
import de.mm20.launcher2.ui.launcher.search.common.list.ListResults
import de.mm20.launcher2.ui.layout.BottomReversed
import de.mm20.launcher2.ui.locals.LocalGridSettings

/**
 * Icon-only, right aligned variant of the profile tab row.
 */
@Composable
private fun DiscreetProfileSwitcher(
    profiles: List<Profile>,
    selectedProfileType: Profile.Type,
    onProfileSelected: (Profile) -> Unit,
    showLockButton: Boolean,
    onLock: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLockButton) {
            IconButton(onClick = onLock) {
                Icon(
                    painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_off_20px else R.drawable.lock_20px),
                    contentDescription = stringResource(
                        if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_action_lock
                        else R.string.profile_private_profile_action_lock
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (profile in profiles) {
            val selected = profile.type == selectedProfileType
            IconToggleButton(
                checked = selected,
                onCheckedChange = { onProfileSelected(profile) },
            ) {
                Icon(
                    painterResource(
                        when (profile.type) {
                            Profile.Type.Personal -> if (selected) R.drawable.person_24px_filled else R.drawable.person_24px
                            Profile.Type.Work -> if (selected) R.drawable.enterprise_24px_filled else R.drawable.enterprise_24px
                            Profile.Type.Private -> if (selected) R.drawable.encrypted_24px_filled else R.drawable.encrypted_24px
                        }
                    ),
                    contentDescription = when (profile.type) {
                        Profile.Type.Personal -> stringResource(R.string.apps_profile_main)
                        Profile.Type.Work -> stringResource(R.string.apps_profile_work)
                        Profile.Type.Private -> stringResource(R.string.apps_profile_private)
                    },
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun LazyListScope.AppResults(
    onProfileSelected: (Profile) -> Unit = {},
    profiles: List<Profile> = emptyList(),
    selectedProfile: Profile? = null,
    profileStates: Map<Profile.Type, Profile.State> = emptyMap(),
    showProfileLockControls: Boolean = false,
    discreetProfileSwitcher: Boolean = false,
    onProfileLockChange: ((Profile, Boolean) -> Unit)? = null,
    apps: List<Application>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    highlightedItem: Application? = null,
    columns: Int,
    reverse: Boolean,
    showList: Boolean,
) {

    val profileIndex by derivedStateOf {
        profiles.indexOf(selectedProfile).takeIf { it >= 0 } ?: 0
    }
    val selectedProfileType by derivedStateOf {
        selectedProfile?.type ?: Profile.Type.Personal
    }

    val isProfileLocked by derivedStateOf {
        profileStates[selectedProfileType]?.locked == true
    }

    val before = if (profiles.size > 1) {
         @Composable {
            Column(
                verticalArrangement = if (reverse) Arrangement.BottomReversed else Arrangement.Top,
            ) {
                if (discreetProfileSwitcher) {
                    DiscreetProfileSwitcher(
                        profiles = profiles,
                        selectedProfileType = selectedProfileType,
                        onProfileSelected = onProfileSelected,
                        showLockButton = showProfileLockControls && !isProfileLocked &&
                                selectedProfile != null && selectedProfileType != Profile.Type.Personal,
                        onLock = {
                            selectedProfile?.let { onProfileLockChange?.invoke(it, true) }
                        },
                    )
                } else
                PrimaryScrollableTabRow(
                    selectedTabIndex = profileIndex,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    for (profile in profiles) {
                        val selected = profile.type == selectedProfileType
                        LeadingIconTab(
                            selected = selected,
                            text = {
                                Text(
                                    when (profile.type) {
                                        Profile.Type.Personal -> stringResource(R.string.apps_profile_main)
                                        Profile.Type.Work -> stringResource(R.string.apps_profile_work)
                                        Profile.Type.Private -> stringResource(R.string.apps_profile_private)
                                    }
                                )
                            },
                            icon = {
                                when (profile.type) {
                                    Profile.Type.Personal -> Icon(
                                        painterResource(if (selected) R.drawable.person_24px_filled else R.drawable.person_24px),
                                        contentDescription = null
                                    )

                                    Profile.Type.Work -> Icon(
                                        painterResource(if (selected) R.drawable.enterprise_24px_filled else R.drawable.enterprise_24px),
                                        contentDescription = null
                                    )

                                    Profile.Type.Private -> Icon(
                                        painterResource(if (selected) R.drawable.encrypted_24px_filled else R.drawable.encrypted_24px),
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                onProfileSelected(profile)
                            }
                        )
                    }
                }

                if (!discreetProfileSwitcher && (!showList || isProfileLocked)) {
                    HorizontalDivider()
                }

                if (selectedProfile != null && selectedProfile?.type != Profile.Type.Personal) {
                    if (isProfileLocked) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.small
                                )
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    MaterialTheme.shapes.small
                                )
                                .padding(vertical = 64.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_off_48px else R.drawable.lock_48px),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                stringResource(
                                    if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_state_locked
                                    else R.string.profile_private_profile_state_locked
                                ),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (showProfileLockControls) {
                                Button(
                                    modifier = Modifier.padding(top = 32.dp),
                                    onClick = {
                                        onProfileLockChange?.invoke(
                                            selectedProfile!!,
                                            false
                                        )
                                    },
                                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                                ) {
                                    Icon(
                                        painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_20px else R.drawable.lock_open_20px),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(end = ButtonDefaults.IconSpacing)
                                            .size(ButtonDefaults.IconSize)
                                    )
                                    Text(
                                        stringResource(
                                            if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_action_unlock
                                            else R.string.profile_private_profile_action_unlock
                                        )
                                    )
                                }
                            }
                        }
                    } else if (showProfileLockControls && !discreetProfileSwitcher) {
                        FilledTonalButton(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            onClick = {
                                onProfileLockChange?.invoke(
                                    selectedProfile,
                                    true
                                )
                            },
                            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                        ) {
                            Icon(
                                painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_off_20px else R.drawable.lock_20px),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = ButtonDefaults.IconSpacing)
                                    .size(ButtonDefaults.IconSize)
                            )
                            Text(
                                stringResource(
                                    if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_action_lock
                                    else R.string.profile_private_profile_action_lock
                                )
                            )
                        }
                    }
                }
            }
        }
    } else null
    if (showList) {
        ListResults(
            key = "apps",
            items = if (isProfileLocked) emptyList() else apps,
            before = before?.let { { it() } },
            selectedIndex = selectedIndex,
            itemContent = { app, showDetails, index ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    item = app,
                    showDetails = showDetails,
                    onShowDetails = { onSelect(if(it) index else -1) },
                    highlight = highlightedItem?.key == app.key
                )
            },
            reverse = reverse,
        )
    } else {
        GridResults(
            key = "apps",
            items = if (isProfileLocked) emptyList() else apps,
            before = before,
            itemContent = {
                GridItem(
                    item = it,
                    showLabels = LocalGridSettings.current.showLabels,
                    highlight = it.key == highlightedItem?.key
                )
            },
            reverse = reverse,
            columns = columns,
        )
    }

}