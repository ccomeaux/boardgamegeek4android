package com.boardgamegeek.ui.adapter

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Game
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class LinkedCollectionAdapter :
    ListAdapter<CollectionItem, LinkedCollectionAdapter.DetailViewHolder>(
        object : DiffUtil.ItemCallback<CollectionItem>() {
            override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.gameId == newItem.gameId
            override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DetailViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(item: CollectionItem) {
            composeView.setContent {
                CollectionRowItem(
                    thumbnailUrl = item.robustThumbnailUrl,
                    name = item.robustName,
                    yearPublished = item.yearPublished,
                    isFavorite = item.isFavorite,
                    infoText = null, //item.infoText,
                    rating = item.rating,
                    timestamp = null,
                ) {
                    GameActivity.start(
                        itemView.context,
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

@Composable
private fun CollectionRowItem(
    name: String,
    thumbnailUrl: String,
    yearPublished: Int?,
    isFavorite: Boolean,
    infoText: String?,
    rating: Double?,
    timestamp: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.tallPaddingValues),
    ) {
        ListItemThumbnail(thumbnailUrl)

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ListItemPrimaryText(
                    text = name,
                    modifier = Modifier.weight(1f)
                )
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(id = R.string.menu_favorite),
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val context = LocalContext.current
                if (yearPublished != null && yearPublished != Game.YEAR_UNKNOWN) {
                    ListItemSecondaryText(
                        text = yearPublished.asYear(context),
                        icon = Icons.Outlined.CalendarToday,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (rating != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Rating(
                        rating,
                        style = MaterialTheme.typography.labelMedium,
                        width = RatingDefaults.widthSmall,
                    )
                } else if (infoText != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    ListItemSecondaryText(
                        text = infoText,
                        modifier = Modifier.alignByBaseline()
                    )
                } else if (timestamp != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    var relativeTimestamp by remember {
                        mutableStateOf(
                            timestamp.formatTimestamp(context, includeTime = false).toString()
                        )
                    }
                    ListItemSecondaryText(
                        text = relativeTimestamp,
                        modifier = Modifier.alignByBaseline()
                    )
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(30.seconds)
                            relativeTimestamp = timestamp.formatTimestamp(context, includeTime = false).toString()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollectionRowItemPreview() {
    BggAppTheme {
        CollectionRowItem(
            name = "Gaia Project",
            thumbnailUrl = "",
            yearPublished = 2012,
            isFavorite = true,
            infoText = null,
            rating = 8.0,
            timestamp = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectionRowItemPreviewWithInfo() {
    BggAppTheme {
        CollectionRowItem(
            name = "Another Long Game Name That Might Wrap To Two Lines",
            thumbnailUrl = "",
            yearPublished = 2023,
            isFavorite = false,
            infoText = "Some other information here",
            rating = null,
            timestamp = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectionRowItemPreviewWithTimestamp() {
    BggAppTheme {
        CollectionRowItem(
            name = "Game With Timestamp",
            thumbnailUrl = "",
            yearPublished = 2020,
            isFavorite = true,
            infoText = null,
            rating = null,
            timestamp = System.currentTimeMillis(),
        )
    }
}
