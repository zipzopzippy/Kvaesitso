package de.mm20.launcher2.ui.settings.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import de.mm20.launcher2.ui.component.preferences.CheckboxPreference
import de.mm20.launcher2.ui.component.preferences.PreferenceScreen
import de.mm20.launcher2.ui.ktx.toPixels
import kotlinx.serialization.Serializable

@Serializable
data object FrequentlyUsedAppsSettingsRoute : NavKey

@Composable
fun FrequentlyUsedAppsSettingsScreen() {
    val viewModel: FrequentlyUsedAppsSettingsScreenVM = viewModel()
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    val xsShape = MaterialTheme.shapes.extraSmall
    val mdShape = MaterialTheme.shapes.medium

    PreferenceScreen(
        stringResource(R.string.preference_frequently_used_apps),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val items = apps
        if (items == null) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            itemsIndexed(items, key = { _, it -> it.app.key }) { index, app ->
                Box(
                    modifier = Modifier.clip(
                        when {
                            items.size == 1 -> mdShape
                            index == 0 -> mdShape.copy(
                                bottomEnd = xsShape.bottomEnd,
                                bottomStart = xsShape.bottomStart
                            )

                            index == items.size - 1 -> mdShape.copy(
                                topEnd = xsShape.topEnd,
                                topStart = xsShape.topStart
                            )

                            else -> xsShape
                        }
                    )
                ) {
                    CheckboxPreference(
                        icon = {
                            val size = 32.dp
                            val px = size.toPixels().toInt()
                            val icon by remember(app.app.key, px) {
                                viewModel.getIcon(app.app, px)
                            }.collectAsStateWithLifecycle(null)
                            val badge by remember(app.app.key) {
                                viewModel.getBadge(app.app)
                            }.collectAsStateWithLifecycle(null)
                            ShapedLauncherIcon(size = size, icon = { icon }, badge = { badge })
                        },
                        title = app.app.label,
                        value = app.isChecked,
                        onValueChanged = {
                            viewModel.onAppChecked(app, it)
                        }
                    )
                }
            }
        }
    }
}
