package com.boardgamegeek.ui.compose

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter

@Composable
fun GameCollectionItemListItem(
    item: CollectionItem,
    modifier: Modifier = Modifier,
    markupConverter: XmlApiMarkupConverter? = null,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val statuses = remember(item) { describeStatuses(item, context).formatList() }
    val description = remember(item) {
        if (item.collectionName.isNotBlank() && item.collectionName != item.gameName ||
            item.collectionYearPublished != CollectionItem.YEAR_UNKNOWN && item.collectionYearPublished != item.gameYearPublished
        ) {
            if (item.collectionYearPublished == CollectionItem.YEAR_UNKNOWN) {
                item.collectionName
            } else {
                "${item.collectionName} (${item.collectionYearPublished.asYear(context)})"
            }
        } else ""
    }

    val commentHtml = remember(item, markupConverter) {
        if (item.comment.isNotBlank() && markupConverter != null) {
            AnnotatedString.fromHtml(markupConverter.toHtml(item.comment, prewrap = false))
        } else AnnotatedString(item.comment)
    }

    val privateCommentHtml = remember(item, markupConverter) {
        if (item.privateComment.isNotBlank() && markupConverter != null) {
            AnnotatedString.fromHtml(markupConverter.toHtml(item.privateComment, prewrap = false))
        } else AnnotatedString(item.privateComment)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItemThumbnail(url = item.thumbnailUrl)

            Column(modifier = Modifier.weight(1f)) {
                if (statuses.isNotEmpty()) {
                    Text(
                        text = statuses,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            if (item.rating in 1.0..10.0) {
                Rating(item.rating) // TODO make larger?
            }
        }

        if (commentHtml.isNotBlank()) {
            Text(
                text = commentHtml,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5
            )
        }

        if (item.hasPrivateInfo()) {
            // TODO:
            // Since getPrivateInfo returns a Spanned with bold parts, we can convert it to HTML and then to AnnotatedString
            // or just use the string if we don't care about bolding in the list. Look at this library first: https://github.com/Aghajari/AnnotatedText
            // For now, let's just use the plain string to keep it simple, or find a way to convert Spanned to AnnotatedString.
            Text(
                text = item.getPrivateInfo(context).toString(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }

        if (privateCommentHtml.isNotBlank()) {
            Text(
                text = privateCommentHtml,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5
            )
        }
    }
}

private fun describeStatuses(item: CollectionItem, context: Context): List<String> {
    val statuses = mutableListOf<String>()
    if (item.own) statuses.add(context.getString(R.string.collection_status_own))
    if (item.previouslyOwned) statuses.add(context.getString(R.string.collection_status_prev_owned))
    if (item.forTrade) statuses.add(context.getString(R.string.collection_status_for_trade))
    if (item.wantInTrade) statuses.add(context.getString(R.string.collection_status_want_in_trade))
    if (item.wantToBuy) statuses.add(context.getString(R.string.collection_status_want_to_buy))
    if (item.wantToPlay) statuses.add(context.getString(R.string.collection_status_want_to_play))
    if (item.preOrdered) statuses.add(context.getString(R.string.collection_status_preordered))
    if (item.wishList) {
        statuses.add(
            context.resources.getStringArray(R.array.wishlist_priority).getOrNull(item.wishListPriority) ?: context.getString(R.string.wishlist)
        )
    }
    if (statuses.isEmpty()) {
        if (item.numberOfPlays > 0) {
            statuses.add(context.getString(R.string.played))
        } else {
            if (item.rating > 0.0) statuses.add(context.getString(R.string.rated))
            if (item.comment.isNotBlank()) statuses.add(context.getString(R.string.commented))
        }
    }
    return statuses
}

@Preview(showBackground = true)
@Composable
private fun CollectionItemRowPreview() {
    BggAppTheme {
        GameCollectionItemListItem(
            item = CollectionItem(
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
        )
    }
}
