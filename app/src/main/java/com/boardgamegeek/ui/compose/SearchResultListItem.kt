package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun SearchResultListItem(
    searchResult: SearchResult,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onLongClick = onLongClick,
            ) {
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .then(modifier)
    ) {
        val titleStyle = if (searchResult.nameType == SearchResult.NameType.Alternate)
            MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic)
        else
            MaterialTheme.typography.titleMedium
        val variantStyle = if (searchResult.nameType == SearchResult.NameType.Alternate)
            MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
        else
            MaterialTheme.typography.bodyMedium
        Text(
            searchResult.name,
            style = titleStyle,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = searchResult.yearPublished.asYear(LocalContext.current),
                style = variantStyle,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.id_list_text, searchResult.id),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchResultListItemPreview(
    @PreviewParameter(SearchResultPreviewParameterProvider::class) searchResult: SearchResult
) {
    BggAppTheme {
        SearchResultListItem(
            searchResult,
            isSelected = true,
        )
    }
}

private class SearchResultPreviewParameterProvider : PreviewParameterProvider<SearchResult> {
    override val values = sequenceOf(
        SearchResult(
            id = 188,
            name = "Ticket to Ride",
            yearPublished = 2004,
            nameType = SearchResult.NameType.Primary,
        ),
        SearchResult(
            id = 13,
            name = "CATAN",
            yearPublished = 1996,
            nameType = SearchResult.NameType.Alternate,
        ),
    )
}
