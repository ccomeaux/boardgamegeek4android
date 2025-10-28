package com.boardgamegeek.ui.compose

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R

@Composable
fun ViewAppBarAction(onView: () -> Unit) {
    IconButton(onClick = { onView() }) {
        Icon(
            painterResource(R.drawable.ic_baseline_open_in_browser_24),
            contentDescription = stringResource(R.string.menu_view),
        )
    }
}

@Composable
fun ShareAppBarAction(onShare: () -> Unit) {
    IconButton(onClick = { onShare() }) {
        Icon(
            painterResource(R.drawable.ic_baseline_share_24),
            contentDescription = stringResource(R.string.menu_share),
        )
    }
}

@Composable
fun LogPlayQuickAppBarAction(onQuickLogPlay: () -> Unit) {
    IconButton(onClick = { onQuickLogPlay() }) {
        Icon(
            painterResource(R.drawable.ic_baseline_event_available_24),
            contentDescription = stringResource(R.string.menu_log_play),
        )
    }
}

@Composable
fun LogPlayAppBarExpandableActions(
    onLogPlay: () -> Unit,
    onLogPlayWizard: () -> Unit,
    onQuickLogPlay: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { expandedMenu = true }) {
        Icon(
            painterResource(R.drawable.ic_baseline_event_available_24),
            contentDescription = stringResource(R.string.menu_log_play),
        )
    }
    DropdownMenu(
        expanded = expandedMenu,
        onDismissRequest = { expandedMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_log_play_short)) },
            onClick = {
                onLogPlay()
                expandedMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_log_play_wizard_short)) },
            onClick = {
                onLogPlayWizard()
                expandedMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_log_play_quick_short)) },
            onClick = {
                onQuickLogPlay()
                expandedMenu = false
            }
        )
    }
}
