package com.boardgamegeek.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GameViewModel

@Composable
fun GameLinkedItemsScreen(
    game: Game?,
    baseGames: List<GameDetail>?,
    expansions: List<GameDetail>?,
    modifier: Modifier = Modifier,
    onItemClick: (GameDetail) -> Unit = {},
    onMoreClick: (String, GameViewModel.ProducerType) -> Unit = { _, _ -> },
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        val count = (baseGames?.size ?: 0) + (expansions?.size ?: 0)
        if ((game == null || count == 0)) {
            EmptyContent(
                stringResource(R.string.empty_game),
                painterResource(R.drawable.link_24px),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            if (!baseGames.isNullOrEmpty()) {
                val headerText = stringResource(R.string.base_games)
                GameDetailsFlowRow(
                    list = baseGames,
                    headerText = headerText,
                    moreButtonIconId = R.drawable.ic_baseline_flip_to_front_24,
                    onItemClick = { onItemClick(it) },
                    onMoreClick = { onMoreClick(headerText, GameViewModel.ProducerType.BASE_GAME) },
                )
            }
            if (!expansions.isNullOrEmpty()) {
                val headerText = stringResource(R.string.expansions)
                GameDetailsFlowRow(
                    list = expansions,
                    headerText = headerText,
                    moreButtonIconId = R.drawable.ic_baseline_flip_to_front_24,
                    onItemClick = { onItemClick(it) },
                    onMoreClick = { onMoreClick(headerText, GameViewModel.ProducerType.EXPANSION) },
                )
            }
            Spacer(Modifier.heightIn(8.dp))
            HorizontalDivider()
            GameFooter(game.updated, game.id)
        }
    }
}

@Preview
@Composable
private fun GameLinkedItemsScreenEmptyPreview() {
    BggAppTheme {
        GameLinkedItemsScreen(
            Game(123, "Ticket to Ride"),
            emptyList(),
            emptyList(),
        )
    }
}

@Preview
@Composable
private fun GameLinkedItemsScreenPreview() {
    BggAppTheme {
        GameLinkedItemsScreen(
            Game(123, "Ticket to Ride"),
            listOf(
                GameDetail(456, "Ticket to Ride 2"),
            ),
            emptyList(),
        )
    }
}