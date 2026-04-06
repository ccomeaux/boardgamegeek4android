package com.boardgamegeek.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R

@Composable
fun SubtitleWithIcon(text: String, painter: Painter) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp)
        )
        Text(text)
    }
}

@Composable
fun UpAppBarAction(onUpClick: () -> Unit) {
    IconButton(onClick = { onUpClick() }) {
        Icon(
            painterResource(R.drawable.arrow_back_24px),
            contentDescription = stringResource(R.string.up)
        )
    }
}

@Composable
fun ViewAppBarAction(onView: () -> Unit) {
    IconButton(onClick = { onView() }) {
        Icon(
            painterResource(R.drawable.open_in_browser_24px),
            contentDescription = stringResource(R.string.menu_view_in_browser),
        )
    }
}

@Composable
fun ShareAppBarAction(onShare: () -> Unit) {
    IconButton(onClick = { onShare() }) {
        Icon(
            painterResource(R.drawable.share_24px),
            contentDescription = stringResource(R.string.menu_share),
        )
    }
}

@Composable
fun LogPlayQuickAppBarAction(onQuickLogPlay: () -> Unit) {
    IconButton(onClick = { onQuickLogPlay() }) {
        Icon(
            painterResource(R.drawable.log_play_24px),
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
    var isMenuExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { isMenuExpanded = true }) {
        Icon(
            painterResource(R.drawable.log_play_24px),
            contentDescription = stringResource(R.string.menu_log_play),
        )
    }
    DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = { isMenuExpanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_log_play_short)) },
            onClick = {
                onLogPlay()
                isMenuExpanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_log_play_wizard_short)) },
            onClick = {
                onLogPlayWizard()
                isMenuExpanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_log_play_quick_short)) },
            onClick = {
                onQuickLogPlay()
                isMenuExpanded = false
            }
        )
    }
}
