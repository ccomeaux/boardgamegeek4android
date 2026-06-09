package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.compose.SmallRating

@Composable
fun BrowseScreen(
    recentlyViewedItems: List<CollectionItem>?,
    friendlessFavorites: List<CollectionItem>?,
    friendless: Int?,
    underratedItems: List<CollectionItem>?,
    modifier: Modifier = Modifier,
) {
    val headerModifier = Modifier.padding(horizontal = 16.dp)
    Column(
        modifier = modifier
    ) {
        recentlyViewedItems?.let {
            if (it.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_recently_viewed),
                    modifier = headerModifier,
                )
                Shelf(it)
            }
        }

        friendlessFavorites?.let {
            SectionHeader(
                stringResource(R.string.title_friendless_favorites),
                infoText = stringResource(R.string.play_stat_friendless_info),
                count = friendless,
                modifier = headerModifier,
            )
            Shelf(
                it,
                badgeContent = { item -> SmallRating(item.rating) },
            )
        }

        underratedItems?.let {
            SectionHeader(
                stringResource(R.string.title_hidden_gems),
                infoText = stringResource(R.string.info_hidden_gems),
                modifier = headerModifier,
            )
            Shelf(
                it,
                badgeContent = { item -> SmallRating(item.rating) },
            )
        }
    }
}
