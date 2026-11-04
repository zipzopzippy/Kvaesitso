package de.mm20.launcher2.ui.launcher.searchbar

import android.content.Intent
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.settings.SettingsActivity

@Composable
fun RowScope.SearchBarMenu(
    searchBarValue: String,
    onInputClear: () -> Unit,
) {
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }
    val rightIcon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_ic_menu_clear)

    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(
            onClick = {
                if (searchBarValue.isNotBlank()) onInputClear()
                else showOverflowMenu = true
            },
        ) {
            Icon(
                painter = rememberAnimatedVectorPainter(
                    rightIcon,
                    atEnd = searchBarValue.isNotEmpty()
                ),
                contentDescription = stringResource(if (searchBarValue.isNotBlank()) R.string.action_clear else R.string.action_more_actions),
                tint = LocalContentColor.current
            )
        }
        DropdownMenuPopup(
            expanded = showOverflowMenu,
            onDismissRequest = { showOverflowMenu = false },
            // Personal build: a focusable popup makes Android drop the keyboard without animating,
            // which makes the search UI jump (see MODIFICATIONS.md)
            properties = PopupProperties(focusable = false),
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start)
            ) {
                DropdownMenuItem(
                    shape = MenuDefaults.leadingItemShape,
                    onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SET_WALLPAPER),
                                null
                            )
                        )
                        showOverflowMenu = false
                    },
                    text = {
                        Text(stringResource(R.string.wallpaper))
                    },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.wallpaper_24px), contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    shape = MenuDefaults.trailingItemShape,
                    onClick = {
                        context.startActivity(
                            Intent(context, SettingsActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        showOverflowMenu = false
                    },
                    text = {
                        Text(stringResource(R.string.settings))
                    },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.settings_24px), contentDescription = null)
                    }
                )
            }
        }
    }
}