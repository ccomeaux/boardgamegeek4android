package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun GameFooter(syncTimestamp: Long, gameId: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val context = LocalContext.current
        val never = stringResource(R.string.needs_updating)
        val prefix = stringResource(R.string.sync)
        var relativeTimestamp by remember {
            mutableStateOf(never)
        }
        if (syncTimestamp > 0L) {
            LaunchedEffect(Unit) {
                while (true) {
                    relativeTimestamp = "$prefix ${syncTimestamp.formatTimestamp(context, includeTime = true, isForumTimestamp = false)}"
                    delay(30.seconds)
                }
            }
        }
        Text(relativeTimestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$gameId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun GameFooterPreview() {
    BggAppTheme {
        GameFooter(System.currentTimeMillis(), 12345, Modifier.fillMaxWidth())
    }
}
