package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.model.CollectionItem

@Composable
fun OwnScreen(
    growthRate: Int?,
    utilization: Double?,
    ownedItems: Pair<List<CollectionItem>, Int>?,
    ownedExpansions: Pair<List<CollectionItem>, Int>?,
    ownedAccessories: Pair<List<CollectionItem>, Int>?,
    recentlyAcquired: Pair<List<CollectionItem>, Int>?,
    hawt: List<CollectionItem>?,
    modifier: Modifier = Modifier,
) {
    val headerModifier = Modifier.padding(horizontal = 16.dp)
    Column(
        modifier = modifier
    ) {
        growthRate?.let {
            InformationText(stringResource(R.string.msg_growth_rate, it))
        }
        utilization?.let {
            InformationText(stringResource(R.string.msg_utilization, it.asPercentage()))
        }
        ownedItems?.let {
            SectionHeader(
                stringResource(R.string.title_owned_games),
                count = it.second,
                modifier = headerModifier,
            )
            Shelf(it.first)
        }
        ownedExpansions?.let {
            SectionHeader(
                stringResource(R.string.title_owned_expansions),
                count = it.second,
                modifier = headerModifier,
            )
            Shelf(it.first)
        }
        ownedAccessories?.let {
            if (it.second > 0) {
                SectionHeader(
                    stringResource(R.string.title_owned_accessories),
                    count = it.second,
                    modifier = headerModifier,
                )
                Shelf(it.first)
            }
        }
        recentlyAcquired?.let {
            SectionHeader(
                stringResource(R.string.title_recently_acquired),
                infoText = stringResource(R.string.info_recently_acquired),
                count = it.second,
                modifier = headerModifier,
            )
            Shelf(it.first) { item ->
                SmallBadge(item.acquisitionDate.asPastDaySpan(LocalContext.current).toString())
            }
        }
        hawt?.let {
            SectionHeader(
                stringResource(R.string.title_hawt),
                infoText = stringResource(R.string.info_hawt),
                modifier = headerModifier,
            )
            Shelf(it)
        }
    }
}
