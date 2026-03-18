@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui.game

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.addAlphaToColor
import com.boardgamegeek.extensions.asPlayCount
import com.boardgamegeek.extensions.isKnownColor
import com.boardgamegeek.extensions.isToday
import com.boardgamegeek.extensions.toFormattedString
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.ColorBox
import com.boardgamegeek.ui.compose.ColorBoxDefaults
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.PlayListItem
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun GamePlaysScreen(
    game: Game,
    updatedTimestamp: Long,
    plays: List<Play>,
    colors: List<String>,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onPlaysClick: () -> Unit = {},
    onPlayClick: (Play) -> Unit = {},
    onStatsClick: () -> Unit = {},
    onColorsClick: () -> Unit = {},
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        val iconColor = Color(game.iconColor.addAlphaToColor())
        if (isRefreshing) {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }
        TotalPlaysRow(plays) {
            onPlaysClick()
        }
        val inProgressPlays = plays.filter { play -> play.dirtyTimestamp > 0L }
        if (inProgressPlays.isNotEmpty()) {
            InProgressPlaysRow(inProgressPlays, iconColor) { play ->
                onPlayClick(play)
            }
        }
        plays.filter { play -> play.dirtyTimestamp == 0L }.maxByOrNull { play -> play.dateInMillis }?.let { lastPlay ->
            Text(
                text = stringResource(R.string.last_played),
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 56.dp)
                    .padding(top = 8.dp),
            )
            LastPlayRow(lastPlay, iconColor) { onPlayClick(lastPlay) }
        }
        if (plays.isNotEmpty()) {
            StatsRow(iconColor) {
                onStatsClick()
            }
        }
        ColorsRow(colors, iconColor) {
            onColorsClick()
        }
        GameFooter(
            updatedTimestamp,
            game.id,
        )
    }
}

@Composable
private fun TotalPlaysRow(
    plays: List<Play>,
    onClick: () -> Unit = { },
) {
    val playCount = plays.sumOf { it.quantity }
    val (count, description, color) = playCount.asPlayCount(LocalContext.current)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(dimensionResource(R.dimen.game_row_height))
            .clickable(onClick = onClick)
    ) {
        ColorBox(
            color,
            size = ColorBoxDefaults.sizeSmall,
            modifier = Modifier.padding(start = 0.dp, end = 24.dp),
        ) {
            Text(count.toFormattedString(), color = it, modifier = Modifier.align(Alignment.Center))
        }
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .heightIn(dimensionResource(R.dimen.game_row_height)),
            verticalArrangement = Arrangement.Center,
        ) {
            PrimaryRowText(pluralStringResource(R.plurals.play_title_suffix, playCount, playCount))
            if (description.isNotBlank()) SecondaryRowText(description)
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun InProgressPlaysRow(
    plays: List<Play>,
    iconColor: Color = Color.Transparent,
    onClick: (Play) -> Unit = { },
) {
    GameRow(
        R.drawable.ic_outline_timer_24,
        R.string.title_in_progress,
        iconColor = iconColor,
    ) {
        Column {
            plays.take(5).forEach { play ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clickable(onClick = { onClick(play) })
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val context = LocalContext.current
                        var headline by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            while (true) {
                                headline = if (play.startTime > 0) {
                                    context.getString(
                                        R.string.playing_for_prefix,
                                        DateUtils.formatElapsedTime((System.currentTimeMillis() - play.startTime) / 1000)
                                    )
                                } else if (play.dateInMillis.isToday()) {
                                    context.getString(R.string.playing_prefix, play.dateForDisplay(context))
                                } else {
                                    context.getString(R.string.playing_since_prefix, play.dateForDisplay(context))
                                }
                                delay(30.seconds)
                            }
                        }
                        ListItemPrimaryText(headline)
                        ListItemSecondaryText(play.describe(LocalContext.current, false))
                    }
                }
            }
        }
    }
}

@Composable
private fun LastPlayRow(
    play: Play,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = { },
) {
    GameRow(
        R.drawable.plays_24px,
        R.string.last_played,
        Modifier.clickable(onClick = onClick),
        iconColor = iconColor,
    ) {
        PlayListItem(
            play,
            showGameName = false,
            padding = PaddingValues(0.dp),
            minimumHeight = ListItemDefaults.oneLineHeight,
        )
    }
}

@Composable
private fun StatsRow(
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = { },
) {
    GameRow(
        R.drawable.play_stats_24px,
        R.string.title_play_stats,
        Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
            }
            .clickable(onClick = onClick),
        iconColor = iconColor,
    ) {
        PrimaryRowText(stringResource(R.string.title_play_stats))
    }
}

@Composable
private fun ColorsRow(
    colors: List<String>,
    iconColor: Color = Color.Transparent,
    onClick: () -> Unit = { },
) {
    GameRow(
        R.drawable.color_24px,
        R.string.colors,
        Modifier.clickable(onClick = onClick),
        iconColor = iconColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryRowText(
                pluralStringResource(R.plurals.colors_suffix, colors.size, colors.size),
                modifier = Modifier.weight(1f),
            )
            if (colors.isNotEmpty() && colors.all { it.isKnownColor() }) {
                colors.forEach {
                    ColorBox(it, size = ColorBoxDefaults.sizeSmall)
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun GamePlaysPreview() {
    val game = Game(
        id = 13,
        name = "CATAN",
        updatedPlays = System.currentTimeMillis(),
        iconColor = 0x0000FF,
    )
    val plays = listOf(
        Play(
            internalId = BggContract.INVALID_ID.toLong(),
            playId = BggContract.INVALID_ID,
            dateInMillis = System.currentTimeMillis(),
            gameId = game.id,
            gameName = game.name,
            quantity = 1,
            length = 46,
            location = "House",
        ),
        Play(
            dateInMillis = System.currentTimeMillis(),
            gameId = game.id,
            gameName = game.name,
        ),
        Play(
            internalId = BggContract.INVALID_ID.toLong(),
            playId = BggContract.INVALID_ID,
            dateInMillis = System.currentTimeMillis(),
            gameId = game.id,
            gameName = game.name,
            quantity = 1,
            location = "Library",
            startTime = System.currentTimeMillis() - 12345L,
            dirtyTimestamp = System.currentTimeMillis(),
        ),
        Play(
            internalId = BggContract.INVALID_ID.toLong(),
            playId = BggContract.INVALID_ID,
            dateInMillis = System.currentTimeMillis(),
            gameId = game.id,
            gameName = game.name,
            quantity = 1,
            length = 127,
            dirtyTimestamp = System.currentTimeMillis(),
        ),
        Play(
            internalId = BggContract.INVALID_ID.toLong(),
            playId = BggContract.INVALID_ID,
            dateInMillis = System.currentTimeMillis() - 99999999L,
            gameId = game.id,
            gameName = game.name,
            quantity = 1,
            location = "House",
            length = 98,
            dirtyTimestamp = 1L,
        ),
    )
    Column {
        GamePlaysScreen(
            game,
            game.updatedPlays,
            plays = plays,
            colors = listOf("Blue", "Yellow"),
            isRefreshing = true,
        )
    }
}
