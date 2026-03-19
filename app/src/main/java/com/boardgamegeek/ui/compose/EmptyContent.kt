package com.boardgamegeek.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun EmptyFullSizeScrollableContent(
    @StringRes textResource: Int,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    padding: PaddingValues = PaddingValues(0.dp),
    extraContent: @Composable ColumnScope.() -> Unit = {},
) = EmptyContent(
    text = stringResource(textResource),
    iconPainter = iconPainter,
    modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(padding),
    extraContent,
)

@Composable
fun EmptyFullSizeScrollableContent(
    text: String,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    padding: PaddingValues = PaddingValues(0.dp),
) = EmptyContent(
    text = text,
    iconPainter = iconPainter,
    modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(padding)
)

@Composable
fun EmptyFullSizeScrollableContent(
    @StringRes textResource: Int,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    padding: PaddingValues = PaddingValues(0.dp),
) = EmptyContent(
    text = stringResource(textResource),
    iconPainter = iconPainter,
    modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(padding)
)

@Composable
fun EmptyContent(
    text: String,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.wrapContentSize(),
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(108.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        extraContent.invoke(this)
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 600, heightDp = 600)
@Composable
private fun EmptyContentPreview() {
    BggAppTheme {
        EmptyContent(
            stringResource(R.string.empty_forum),
            painterResource(R.drawable.forum_24px),
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun EmptyContentFullSizeScrollablePreview() {
    BggAppTheme {
        EmptyFullSizeScrollableContent(
            R.string.search_initial_help,
            painterResource(R.drawable.ic_twotone_comment_48),
            Modifier.padding(0.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun EmptyListContentPreviewLightDark() {
    BggAppTheme {
        EmptyFullSizeScrollableContent(
            R.string.empty_comments,
            painterResource(R.drawable.ic_twotone_comment_48),
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun EmptyContentExtraContentPreview() {
    BggAppTheme {
        EmptyFullSizeScrollableContent(
            R.string.empty_buddies,
            painterResource(R.drawable.thumb_up_24px),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Extra content")
        }
    }
}