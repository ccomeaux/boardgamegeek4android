package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGamePlaysBinding
import com.boardgamegeek.extensions.addAlphaToColor
import com.boardgamegeek.extensions.asPlayCount
import com.boardgamegeek.extensions.isKnownColor
import com.boardgamegeek.extensions.isToday
import com.boardgamegeek.extensions.toFormattedString
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class GamePlaysFragment : Fragment() {
    private var _binding: FragmentGamePlaysBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGamePlaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshPlays() }

        binding.composeView.setContent {
            val viewModel by activityViewModels<GameViewModel>()
            val game by viewModel.game.observeAsState()
            val plays by viewModel.plays.observeAsState(emptyList())
            val colors by viewModel.playColors.observeAsState(emptyList())
            val isRefreshing by viewModel.playsAreRefreshing.observeAsState(false)

            binding.swipeRefresh.isRefreshing = isRefreshing
            if (isRefreshing) {
                BggLoadingIndicatorBox()
            }

            game?.let { game ->
                if (game.id == BggContract.INVALID_ID) {
                    ErrorContent(
                        stringResource(R.string.invalid_id),
                        painterResource(R.drawable.game_24px)
                    )
                } else {
                    Column {
                        val iconColor = Color(game.iconColor.addAlphaToColor())
                        Spacer(Modifier.height(8.dp))
                        TotalPlaysRow(plays) {
                            GamePlaysActivity.start(
                                requireContext(),
                                game.id,
                                game.name,
                                game.heroImageUrl,
                                game.thumbnailUrl,
                                game.customPlayerSort,
                                game.iconColor,
                            )
                        }
                        val inProgressPlays = plays.filter { play -> play.dirtyTimestamp > 0L }
                        if (inProgressPlays.isNotEmpty()) {
                            InProgressPlaysRow(inProgressPlays, iconColor) { play ->
                                PlayActivity.start(requireContext(), play.internalId)
                            }
                        }
                        plays.filter { play -> play.dirtyTimestamp == 0L }.maxByOrNull { play -> play.dateInMillis }?.let { lastPlay ->
                            LastPlayRow(lastPlay, iconColor) {
                                PlayActivity.start(requireContext(), lastPlay.internalId)
                            }
                        }
                        if (plays.isNotEmpty()) {
                            StatsRow(iconColor) {
                                GamePlayStatsActivity.start(requireContext(), game.id, game.name, game.iconColor)
                            }
                        }
                        ColorsRow(colors, iconColor) {
                            GameColorsActivity.start(requireContext(), game.id, game.name, game.iconColor)
                        }
                        GameFooter(
                            game.updatedPlays,
                            game.id,
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                        )
                    }
                }
            } ?: EmptyContent(
                stringResource(R.string.empty_plays_game),
                painterResource(R.drawable.game_24px),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Composable
fun TotalPlaysRow(
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
            .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
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
        SecondaryRowText(stringResource(R.string.last_played))
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
    Column {
        val iconColor = Color.Blue
        TotalPlaysRow(emptyList())
        TotalPlaysRow(
            listOf(
                Play(
                    dateInMillis = System.currentTimeMillis(),
                    gameId = 13,
                    gameName = "Terra Mystica",
                ),
            ),
        )
        InProgressPlaysRow(
            listOf(
                Play(
                    internalId = BggContract.INVALID_ID.toLong(),
                    playId = BggContract.INVALID_ID,
                    dateInMillis = System.currentTimeMillis(),
                    gameId = 13,
                    gameName = "Terra Mystica",
                    quantity = 1,
                    location = "Library",
                    startTime = System.currentTimeMillis() - 12345L
                ),
                Play(
                    internalId = BggContract.INVALID_ID.toLong(),
                    playId = BggContract.INVALID_ID,
                    dateInMillis = System.currentTimeMillis(),
                    gameId = 13,
                    gameName = "7th Continent",
                    quantity = 1,
                    length = 127,
                ),
                Play(
                    internalId = BggContract.INVALID_ID.toLong(),
                    playId = BggContract.INVALID_ID,
                    dateInMillis = System.currentTimeMillis() - 99999999L,
                    gameId = 13,
                    gameName = "HUANG",
                    quantity = 1,
                    location = "House",
                    length = 98,
                ),
            ),
            iconColor
        )
        LastPlayRow(
            Play(
                internalId = BggContract.INVALID_ID.toLong(),
                playId = BggContract.INVALID_ID,
                dateInMillis = System.currentTimeMillis(),
                gameId = 13,
                gameName = "Catan",
                quantity = 1,
                length = 46,
                location = "House",
            ),
            iconColor,
        )
        StatsRow(iconColor)
        ColorsRow(listOf("Blue", "Yellow"), iconColor)
    }
}