package com.boardgamegeek.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.compose.BggLoadingIndicator
import com.boardgamegeek.ui.compose.CollectionItemListItem
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun SimpleCollectionItemList(
    collectionItems: List<CollectionItem>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    emptyTextResource: Int = R.string.empty_games,
    onItemClick: (CollectionItem) -> Unit = {},
) {
    when {
        collectionItems == null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                BggLoadingIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.padding_extra))
                )
            }
        }
        collectionItems.isEmpty() -> {
            EmptyContent(
                emptyTextResource,
                Icons.Default.Collections,
                modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                items(
                    items = collectionItems,
                ) {
                    CollectionItemListItem(
                        name = it.collectionName,
                        thumbnailUrl = it.robustThumbnailUrl,
                        yearPublished = it.yearPublished,
                        rating = it.rating,
                    ) { onItemClick(it) }
                }
            }
        }
    }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
fun SimpleCollectionItemListPreview_Loading() {
    BggAppTheme {
        SimpleCollectionItemList(
            collectionItems = null,
        )
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
fun SimpleCollectionItemListPreview_Empty() {
    BggAppTheme {
        SimpleCollectionItemList(
            collectionItems = emptyList(),
        )
    }
}

@Preview(name = "Data State", showBackground = true)
@Composable
fun SimpleCollectionItemListPreview_WithData() {
    // Sample data for the preview
    val sampleItems = listOf(
        CollectionItem(
            internalId = 1,
            collectionName = "Wingspan",
            thumbnailUrl = "https://cf.geekdo-images.com/thumb/img/dJZtZZ_HYizsBWQRVq2HEnqP05Q=/fit-in/200x150/pic4458123.jpg", // Example URL
            gameYearPublished = 2019,
            rating = 8.1
        ),
        CollectionItem(
            internalId = 2,
            collectionName = "Gloomhaven",
            thumbnailUrl = "https://cf.geekdo-images.com/thumb/img/GRUI_Kmd5AlqZrE1oZ3SYR2H26k=/fit-in/200x150/pic2437871.jpg", // Example URL
            gameYearPublished = 2017,
            rating = 8.7
        ),
        CollectionItem(
            internalId = 3,
            collectionName = "Terraforming Mars",
            thumbnailUrl = "https://cf.geekdo-images.com/thumb/img/96PAE0Rl0X2G17wL9L82uS2s7XU=/fit-in/200x150/pic3536616.jpg", // Example URL
            gameYearPublished = 2016,
            rating = 8.4
        )
    )
    MaterialTheme {
        SimpleCollectionItemList(
            collectionItems = sampleItems,
            contentPadding = PaddingValues(all = 16.dp),
        )
    }
}