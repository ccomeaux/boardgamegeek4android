package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.asScore
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.compose.SmallRating
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import java.text.DecimalFormat

@Composable
fun AnalyzeScreen(
    stats: CollectionDetailsViewModel.CollectionAnalyzeStats?,
    ratableItems: Pair<List<CollectionItem>, Int>?,
    commentableItems: Pair<List<CollectionItem>, Int>?,
    modifier: Modifier = Modifier,
    onUpdateRating: (Long, Double) -> Unit = { _, _ -> },
    onUpdateComment: (Long, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val headerModifier = Modifier.padding(top = 16.dp)
    Column(
        modifier = modifier,
    ) {
        stats?.let {
            val displayFormat = DecimalFormat("0.00")
            Text(
                stringResource(
                    R.string.collection_analyze_summary,
                    it.averagePersonalRating.asPersonalRating(context),
                    it.averageAverageRating.asBoundedRating(context, displayFormat),
                    it.correlationCoefficient.asScore(context, format = displayFormat)
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        ratableItems?.let {
            SectionHeader(
                stringResource(R.string.title_games_to_rate),
                count = it.second,
                modifier = headerModifier,
            )
            Shelf(
                it.first,
                onRateClick = { internalId, rating -> onUpdateRating(internalId, rating) }
            ) { item ->
                SmallBadge(
                    pluralStringResource(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays)
                )
            }
        }
        commentableItems?.let {
            SectionHeader(
                stringResource(R.string.title_games_to_comment),
                count = it.second,
                modifier = headerModifier,
            )
            Shelf(
                it.first,
                onCommentClick = { internalId, comment -> onUpdateComment(internalId, comment) }
            ) { item ->
                SmallRating(item.rating)
            }
        }
    }
}