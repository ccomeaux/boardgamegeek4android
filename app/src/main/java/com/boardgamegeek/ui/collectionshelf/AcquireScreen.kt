package com.boardgamegeek.ui.collectionshelf

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.extensions.asWishListPriority
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.compose.SmallRating
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel

@Composable
fun AcquireScreen(
    collectionAcquireStats: CollectionDetailsViewModel.CollectionAcquireStats?,
    syncStatuses: Set<CollectionStatus>?,
    preordered: Pair<List<CollectionItem>, Int>?,
    wishlist: Pair<List<CollectionItem>, Int>?,
    wantToBuy: Pair<List<CollectionItem>, Int>?,
    wantInTrade: Pair<List<CollectionItem>, Int>?,
    favoriteUnownedItems: List<CollectionItem>?,
    playedButUnownedItems: List<CollectionItem>?,
    hawtUnownedItems: List<CollectionItem>?,
    modifier: Modifier = Modifier,
    acquiredFromList: List<String> = emptyList(),
    onRemoveStatus: (Long, CollectionStatus) -> Unit = { _, _ -> },
    onAcquire: (Long, CollectionDetailsViewModel.AcquisitionInfo) -> Unit = { _, _ -> },
) {
    val dateFormat = DateFormat.getDateFormat(LocalContext.current)
    val headerModifier = Modifier.padding(horizontal = 16.dp)

    Column(
        modifier = modifier,
    ) {
        collectionAcquireStats?.let {
            if (it.incomingCount > 0) {
                InformationText(stringResource(R.string.msg_collection_details_acquire, it.incomingCount, it.futureGrowthRate.asPercentage()))
            }
        }

        if (syncStatuses?.contains(CollectionStatus.Preordered) == true) {
            preordered?.let {
                SectionHeader(
                    stringResource(R.string.collection_status_preordered),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                    onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                    statusToRemove = CollectionStatus.Preordered,
                ) { item -> SmallBadge(dateFormat.format(item.acquisitionDate)) }

            }
        }

        if (syncStatuses?.contains(CollectionStatus.Wishlist) == true) {
            wishlist?.let {
                SectionHeader(
                    stringResource(R.string.collection_status_wishlist),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    badgeContent = { item ->
                        SmallBadge(
                            item.wishListPriority.asWishListPriority(LocalContext.current),
                            backgroundColor = Color(item.wishListPriority.toDouble().toColor(BggColors.fiveStageColors))
                        )
                    },
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                    onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                    statusToRemove = CollectionStatus.Wishlist,
                )
            }
        }

        if (syncStatuses?.contains(CollectionStatus.WantToBuy) == true) {
            wantToBuy?.let {
                SectionHeader(
                    stringResource(R.string.collection_status_want_to_buy),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                    onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                    statusToRemove = CollectionStatus.WantToBuy,
                )
            }
        }

        if (syncStatuses?.contains(CollectionStatus.WantInTrade) == true) {
            wantInTrade?.let {
                SectionHeader(
                    stringResource(R.string.collection_status_want_in_trade),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                    onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                    statusToRemove = CollectionStatus.WantInTrade,
                )
            }
        }

        favoriteUnownedItems?.let {
            if (it.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_favorite_unowned),
                    modifier = headerModifier,
                )
                Shelf(
                    it,
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                ) { item -> SmallRating(item.rating) }
            }
        }

        playedButUnownedItems?.let {
            if (it.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_played_unowned),
                    modifier = headerModifier,
                )
                Shelf(
                    it,
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                ) { item ->
                    SmallBadge(pluralStringResource(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays))
                }
            }
        }

        hawtUnownedItems?.let {
            if (it.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_hawt_unowned),
                    modifier = headerModifier,
                )
                Shelf(
                    it,
                    acquiredFromList = acquiredFromList,
                    onAcquireClick = { internalId, info ->
                        onAcquire(internalId, info)
                    },
                )
            }
        }
    }
}
