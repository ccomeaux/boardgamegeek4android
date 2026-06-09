package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.compose.SmallRating
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel

@Composable
fun Shelf(
    list: List<CollectionItem>,
    acquiredFromList: List<String> = emptyList(),
    onAcquireClick: ((Long, CollectionDetailsViewModel.AcquisitionInfo) -> Unit)? = null,
    onLogPlayClick: ((CollectionItem) -> Unit)? = null,
    onRateClick: ((Long, Double) -> Unit)? = null,
    onCommentClick: ((Long, String) -> Unit)? = null,
    onRemoveStatusClick: ((Long, CollectionStatus) -> Unit)? = null,
    onOfferForTradeClick: ((Long) -> Unit)? = null,
    onTradeClick: ((Long) -> Unit)? = null,
    onAddTradeConditionClick: ((Long, String) -> Unit)? = null,
    statusToRemove: CollectionStatus? = null,
    badgeContent: @Composable (CollectionItem) -> Unit = { item ->
        SmallRating(item.averageRating)
    },
) {
    val context = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        items(
            items = list,
            key = { item -> item.internalId }
        ) { item ->
            CollectionItemCard(
                item,
                acquiredFromList = acquiredFromList,
                badge = { badgeContent(item) },
                onClick = {
                    GameActivity.start(
                        context,
                        item.gameId,
                        item.gameName,
                        item.thumbnailUrl,
                        item.heroImageUrl
                    )
                },
                onAcquire = onAcquireClick?.let {
                    { info -> it(item.internalId, info) }
                },
                onLogPlay = onLogPlayClick?.let {
                    { it(item) }
                },
                onRateClick = onRateClick?.let {
                    { rating -> it(item.internalId, rating) }
                },
                onCommentClick = onCommentClick?.let {
                    { comment -> it(item.internalId, comment) }
                },
                onRemoveStatusClick = onRemoveStatusClick?.let {
                    { status -> it(item.internalId, status) }
                },
                onOfferForTradeClick = onOfferForTradeClick?.let {
                    { it(item.internalId) }
                },
                onTradeClick = onTradeClick?.let {
                    { it(item.internalId) }
                },
                onAddTradeConditionClick = onAddTradeConditionClick?.let {
                    { text -> it(item.internalId, text) }
                },
                statusToRemove = statusToRemove,
            )
        }
    }
}
