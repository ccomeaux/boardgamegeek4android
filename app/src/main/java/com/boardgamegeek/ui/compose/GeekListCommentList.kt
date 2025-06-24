package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter

@Composable
fun GeekListCommentList(
    geekListComments: List<GeekListComment>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
) {
    val markupConverter = XmlApiMarkupConverter(LocalContext.current)
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        state = state,
    ) {
        itemsIndexed(geekListComments) { index, comment ->
            GeekListCommentRow(
                comment,
                markupConverter,
            )
            if (index < geekListComments.lastIndex)
                HorizontalDivider(Modifier)
        }
    }
}

@PreviewLightDark
@Composable
private fun GeekListCommentListPreview() {
    BggAppTheme {
        GeekListCommentList(
            GeekListCommentPreviewParameterProvider().values.toList(),
            contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
        )
    }
}
