package de.mm20.launcher2.ui.common

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.permissions.PermissionGroup
import de.mm20.launcher2.permissions.PermissionsManager
import de.mm20.launcher2.ui.R
import kotlinx.coroutines.flow.combine
import org.koin.compose.koinInject

/**
 * Personal build addition (see MODIFICATIONS.md): after restoring a backup (typically on a fresh
 * install), list the permissions the launcher's main features need that aren't granted yet, each
 * with a button to grant it. Granted ones disappear from the list.
 */
private class RestoredPermission(
    val group: PermissionGroup,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
)

private val RestoredPermissionList = listOf(
    RestoredPermission(PermissionGroup.AppShortcuts, R.string.restore_permission_app_shortcuts, R.drawable.apps_24px),
    RestoredPermission(PermissionGroup.Notifications, R.string.restore_permission_notifications, R.drawable.notifications_24px),
    RestoredPermission(PermissionGroup.Contacts, R.string.restore_permission_contacts, R.drawable.person_24px),
    RestoredPermission(PermissionGroup.Calendar, R.string.restore_permission_calendar, R.drawable.today_24px),
    RestoredPermission(PermissionGroup.Location, R.string.restore_permission_location, R.drawable.location_on_24px),
    RestoredPermission(PermissionGroup.BreezyWeather, R.string.restore_permission_breezy_weather, R.drawable.water_drop_24px_filled),
    RestoredPermission(PermissionGroup.ExternalStorage, R.string.restore_permission_files, R.drawable.folder_24px),
    RestoredPermission(PermissionGroup.ManageProfiles, R.string.restore_permission_profiles, R.drawable.encrypted_24px),
)

@Composable
fun RestoredPermissions(modifier: Modifier = Modifier) {
    val permissionsManager: PermissionsManager = koinInject()
    val activity = LocalActivity.current as? AppCompatActivity ?: return

    val missing by remember {
        combine(RestoredPermissionList.map { permissionsManager.hasPermission(it.group) }) { granted ->
            RestoredPermissionList.filterIndexed { i, _ -> !granted[i] }
        }
    }.collectAsState(emptyList())

    if (missing.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.restore_permissions_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 16.dp),
        )
        for (permission in missing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(permission.icon),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                )
                Text(
                    text = stringResource(permission.label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { permissionsManager.requestPermission(activity, permission.group) }
                ) {
                    Text(stringResource(R.string.restore_permission_grant))
                }
            }
        }
    }
}
