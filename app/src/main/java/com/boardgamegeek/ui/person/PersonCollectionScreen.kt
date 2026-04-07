package com.boardgamegeek.ui.person

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Person.Type
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.compose.CollectionItemListItem
import com.boardgamegeek.ui.compose.EmptyContent

@Composable
fun PersonCollectionScreen(
    collection: List<CollectionItem>?,
    personType: Type,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    if (collection.isNullOrEmpty()) {
        EmptyContent(
            stringResource(
                R.string.empty_linked_collection,
                personType.mapToDescription().lowercase(LocalLocale.current.platformLocale)
            ),
            painterResource(R.drawable.collection_24px),
            modifier.verticalScroll(rememberScrollState())
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = collection,
                key = { collectionItem -> collectionItem.internalId }
            ) { item ->
                CollectionItemListItem(
                    thumbnailUrl = item.robustThumbnailUrl,
                    name = item.robustName,
                    yearPublished = item.yearPublished,
                    isFavorite = item.isFavorite,
                    infoText = null,
                    rating = item.rating,
                    timestamp = null,
                ) {
                    GameActivity.start(
                        context,
                        item.gameId,
                        item.gameName,
                        item.gameThumbnailUrl,
                        item.gameHeroImageUrl
                    )
                }
            }
        }
    }
}
