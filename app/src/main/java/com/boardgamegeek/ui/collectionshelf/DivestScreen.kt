package com.boardgamegeek.ui.collectionshelf

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.compose.SmallRating

@Composable
fun DivestScreen(
    syncStatuses: Set<CollectionStatus>?,
    ownedCount: Int,
    previouslyOwnedRatio: Double,
    forTradeRatio: Double,
    regretFactor: Int?,
    forTrade: Pair<List<CollectionItem>, Int>?,
    forTradeWithoutCondition: Pair<List<CollectionItem>, Int>?,
    previouslyOwned: Pair<List<CollectionItem>, Int>?,
    whyOwnItems: Pair<List<CollectionItem>, Int>?,
    modifier: Modifier = Modifier,
    onRemoveStatus: (Long, CollectionStatus) -> Unit = { _, _ -> },
    onOfferForTrade: (Long) -> Unit = { },
    onTrade: (Long) -> Unit = { },
    onAddTradeCondition: (Long, String) -> Unit = { _, _ -> },
) {
    val dateFormat = DateFormat.getDateFormat(LocalContext.current)
    val headerModifier = Modifier.padding(horizontal = 16.dp)
    Column(
        modifier = modifier,
    ) {
        if ((syncStatuses?.contains(CollectionStatus.Own) == true) && ownedCount > 0) {
            val text = if (syncStatuses.contains(CollectionStatus.PreviouslyOwned)) {
                if (syncStatuses.contains(CollectionStatus.ForTrade)) {
                    stringResource(R.string.msg_collection_details_divest, previouslyOwnedRatio.asPercentage(), forTradeRatio.asPercentage())
                } else {
                    stringResource(R.string.msg_collection_details_divest_previously_owned, previouslyOwnedRatio.asPercentage())
                }
            } else if (syncStatuses.contains(CollectionStatus.ForTrade)) {
                stringResource(R.string.msg_collection_details_divest_for_trade, forTradeRatio.asPercentage())
            } else {
                null
            }
            text?.let {
                InformationText(it)
            }
        }

        regretFactor?.let {
            InformationText(stringResource(R.string.msg_regret_factor, it))
        }

        if (syncStatuses?.contains(CollectionStatus.ForTrade) == true) {
            forTrade?.let {
                SectionHeader(
                    stringResource(R.string.collection_status_for_trade),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    badgeContent = { item -> SmallRating(item.geekRating) },
                    onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                    statusToRemove = CollectionStatus.ForTrade,
                    onTradeClick = { internalId -> onTrade(internalId) },
                )
            }

            forTradeWithoutCondition?.let {
                SectionHeader(
                    stringResource(R.string.title_for_trade_without_condition),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    badgeContent = { item -> SmallRating(item.geekRating) },
                    onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                    statusToRemove = CollectionStatus.ForTrade,
                    // TODO add condition text - modal with text, similar to the AddCommentDialog
                    onTradeClick = { internalId -> onTrade(internalId) },
                    onAddTradeConditionClick = { internalId, text -> onAddTradeCondition(internalId, text) }
                )
            }
        }

        if (syncStatuses?.contains(CollectionStatus.PreviouslyOwned) == true) {
            previouslyOwned?.let {
                SectionHeader(
                    stringResource(R.string.collection_status_prev_owned),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    badgeContent = { item -> SmallRating(item.geekRating) },
                )
            }
        }

        if (syncStatuses?.contains(CollectionStatus.Own) == true) {
            whyOwnItems?.let {
                if (it.first.isNotEmpty()) {
                    SectionHeader(
                        stringResource(R.string.title_why_own),
                        infoText = stringResource(R.string.info_why_own),
                        count = it.second,
                        modifier = headerModifier,
                    )
                    Shelf(
                        it.first,
                        badgeContent = { item -> SmallBadge(item.lastPlayDate?.let { date -> dateFormat.format(date) } ?: "") },
                        onOfferForTradeClick = { internalId -> onOfferForTrade(internalId) },
                        statusToRemove = CollectionStatus.ForTrade,
                    )
                }
            }
        }
    }
}

//    private fun onMenuClick() = { item: CollectionItem, menuItem: MenuItem ->
//        when (menuItem.itemId) {
//            R.id.menu_add_condition_text -> {
//                CollectionDetailsConditionDialogFragment.show(parentFragmentManager, item.gameName, item.internalId, item.conditionText)
//                true
//            }

//        }
//    }
