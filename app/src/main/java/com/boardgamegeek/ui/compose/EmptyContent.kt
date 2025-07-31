package com.boardgamegeek.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun EmptyContent(
    @StringRes textResource: Int,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) = EmptyContent(
    text = stringResource(textResource),
    iconPainter = iconPainter,
    modifier = modifier,
    scrollState = scrollState
)

@Composable
fun EmptyContent(
    text: String,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical)
            ),
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
    }
}

@Composable
fun EmptyContent(
    @StringRes textResource: Int,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) = EmptyContent(
    text = stringResource(textResource),
    imageVector = imageVector,
    modifier = modifier,
    scrollState = scrollState
)

@Composable
fun EmptyContent(
    text: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical)
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(108.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun EmptyContentPreview() {
    BggAppTheme {
        EmptyContent(
            R.string.search_initial_help,
            painterResource(R.drawable.ic_twotone_comment_48),
            Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun EmptyContentPreviewImageVector() {
    BggAppTheme {
        EmptyContent(
            R.string.empty_geeklist,
            Icons.AutoMirrored.Filled.ListAlt,
        )
    }
}

@PreviewLightDark
@Composable
private fun EmptyListContentPreviewLightDark() {
    BggAppTheme {
        EmptyContent(
            R.string.empty_comments,
            painterResource(R.drawable.ic_twotone_comment_48),
        )
    }
}
