@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui.collectionshelf

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.compose.SmallRating
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel

@Composable
fun PlayScreen(
    playerCountType: CollectionDetailsViewModel.PlayerCountType?,
    playerCount: Int?,
    syncStatuses: Set<CollectionStatus>?,
    wantToPlay: Pair<List<CollectionItem>, Int>?,
    recentlyPlayedGames: Pair<List<CollectionItem>, Int>?,
    friendlessShouldPlayGames: Pair<List<CollectionItem>, Int>?,
    shelfOfOpportunityItems: Pair<List<CollectionItem>, Int>?,
    shelfOfNewOpportunityItems: Pair<List<CollectionItem>, Int>?,
    modifier: Modifier = Modifier,
    onLogPlay: (CollectionItem) -> Unit = {},
    onFilterPlayerCountType: (CollectionDetailsViewModel.PlayerCountType) -> Unit = {},
    onFilterPlayerCount: (Int) -> Unit = {},
    onRemoveStatus: (Long, CollectionStatus) -> Unit = { _, _ -> },
) {
    val resources = LocalResources.current
    val headerModifier = Modifier.padding(horizontal = 16.dp)
    Column(
        modifier = modifier,
    ) {
        ButtonGroup(
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            expandedRatio = 0.0f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
        ) {
            toggleableItem(
                checked = (playerCountType == CollectionDetailsViewModel.PlayerCountType.All),
                label = resources.getString(R.string.all),
                onCheckedChange = {
                    onFilterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.All)
                }
            )

            toggleableItem(
                checked = (playerCountType == CollectionDetailsViewModel.PlayerCountType.BestWith),
                label = resources.getString(R.string.best),
                onCheckedChange = {
                    onFilterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.BestWith)
                }
            )

            toggleableItem(
                checked = (playerCountType == CollectionDetailsViewModel.PlayerCountType.GoodWith),
                label = resources.getString(R.string.good),
                onCheckedChange = {
                    onFilterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.GoodWith)
                }
            )

            toggleableItem(
                checked = (playerCountType == CollectionDetailsViewModel.PlayerCountType.Supports),
                label = resources.getString(R.string.supports),
                onCheckedChange = {
                    onFilterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.Supports)
                }
            )
        }

        if (playerCountType != null && playerCountType != CollectionDetailsViewModel.PlayerCountType.All) {
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                expandedRatio = 0.0f,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
            ) {
                for (i in 1..8) {
                    toggleableItem(
                        checked = (i == playerCount),
                        label = "${i}P",
                        onCheckedChange = {
                            if (it) onFilterPlayerCount(i)
                        },
                    )
                }
            }
        }

        if (syncStatuses?.contains(CollectionStatus.WantToPlay) == true) {
            wantToPlay?.let {
                if (it.first.isNotEmpty()) {
                    SectionHeader(
                        stringResource(R.string.collection_status_want_to_play),
                        count = it.second,
                        modifier = headerModifier,
                    )
                    Shelf(
                        it.first,
                        onLogPlayClick = onLogPlay,
                        onRemoveStatusClick = { internalId, status -> onRemoveStatus(internalId, status) },
                        statusToRemove = CollectionStatus.WantToPlay,
                    )
                }
            }
        }

        recentlyPlayedGames?.let {
            if (it.first.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_recently_played),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    onLogPlayClick = onLogPlay
                )
            }
        }

        friendlessShouldPlayGames?.let {
            if (it.first.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_friendless_play),
                    count = it.second,
                    infoText = stringResource(R.string.info_friendless_play),
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    onLogPlayClick = onLogPlay,
                ) { item -> SmallRating(item.rating) }
            }
        }

        shelfOfOpportunityItems?.let {
            if (it.first.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_shelf_of_opportunity),
                    count = it.second,
                    infoText = stringResource(R.string.info_shelf_of_opportunity),
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    onLogPlayClick = onLogPlay,
                )
            }
        }

        shelfOfNewOpportunityItems?.let {
            val dateFormat = DateFormat.getDateFormat(LocalContext.current)
            if (it.first.isNotEmpty()) {
                SectionHeader(
                    stringResource(R.string.title_shelf_of_new_opportunity),
                    count = it.second,
                    infoText = stringResource(R.string.info_shelf_of_new_opportunity),
                    modifier = headerModifier,
                )
                Shelf(
                    it.first,
                    onLogPlayClick = onLogPlay,
                ) { item -> SmallBadge(dateFormat.format(item.acquisitionDate)) }
            }
        }
    }
}
