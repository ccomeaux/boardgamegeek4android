package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.model.Game
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun CollectionItemListItem(
    name: String,
    thumbnailUrl: String,
    yearPublished: Int?,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    infoText: String? = null,
    rating: Double? = null,
    timestamp: Long? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .then(
                if (isSelected)
                    Modifier.clickable(onClick = onClick)
                else
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
            )
            .padding(ListItemDefaults.tallPaddingValues),
    ) {
        ListItemThumbnail(thumbnailUrl)

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ListItemPrimaryText(
                    text = name,
                    modifier = Modifier.weight(1f),
                    isSelected = isSelected,
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
                        modifier = Modifier.alignByBaseline(),
                        isSelected = isSelected,
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
                        modifier = Modifier.alignByBaseline(),
                        isSelected = isSelected,
                    )
                } else if (timestamp != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    val never = stringResource(R.string.never)
                    var relativeTimestamp by remember {
                        mutableStateOf(never)
                    }
                    LaunchedEffect(Unit) {
                        while (true) {
                            relativeTimestamp = timestamp.asPastDaySpan(context).toString()
                            delay(30.seconds)
                        }
                    }
                    ListItemSecondaryText(
                        text = relativeTimestamp,
                        modifier = Modifier.alignByBaseline(),
                        isSelected = isSelected,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollectionItemListItemPreview() {
    BggAppTheme {
        CollectionItemListItem(
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
fun CollectionItemListItemPreviewWithInfo() {
    BggAppTheme {
        CollectionItemListItem(
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
fun CollectionItemListItemPreviewWithTimestamp() {
    BggAppTheme {
        CollectionItemListItem(
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