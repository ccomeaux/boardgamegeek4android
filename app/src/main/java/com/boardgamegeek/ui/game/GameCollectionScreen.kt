package com.boardgamegeek.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Game
import com.boardgamegeek.ui.compose.EmptyFullSizeScrollableContent
import com.boardgamegeek.ui.compose.GameCollectionItemListItem
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter

@Composable
fun GameCollectionScreen(
    game: Game,
    collectionItems: List<CollectionItem>,
    modifier: Modifier = Modifier,
    markupConverter: XmlApiMarkupConverter? = null,
    onItemClicked: (CollectionItem) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        if (collectionItems.isEmpty()) {
            EmptyFullSizeScrollableContent(
                stringResource(R.string.empty_game_collection),
                painterResource(R.drawable.collection_24px),
            )
        } else {
            LazyColumn(modifier = Modifier) {
                items(collectionItems) { item ->
                    GameCollectionItemListItem(item, markupConverter = markupConverter) {
                        onItemClicked(item)
                    }
                }
            }
        }
        GameFooter(game.updated, game.id)
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun GameCollectionScreenEmptyPreview() {
    BggAppTheme {
        GameCollectionScreen(
            Game(123, "Ticket to Ride"),
            emptyList(),
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun GameCollectionScreenPreview() {
    BggAppTheme {
        GameCollectionScreen(
            Game(123, "Ticket to Ride"),
            listOf(
                CollectionItem(
                    gameName = "Gaia Project",
                    thumbnailUrl = "",
                    own = true,
                    rating = 8.5,
                    comment = "Great game!",
                    privateComment = "I should play this more often.",
                    gameYearPublished = 2017,
                    collectionName = "Gaia Project",
                    collectionYearPublished = 2017,
                    pricePaidCurrency = "USD",
                    pricePaid = 49.99,
                    quantity = 2,
                )
            ),
        )
    }
}
