package com.boardgamegeek.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun EmptyContent(@StringRes textResource: Int, iconPainter: Painter, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(108.dp),
            tint = MaterialTheme.colorScheme.secondary // colorResource(R.color.empty_tint),
        )
        Text(
            text = stringResource(textResource),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 320)
@Composable
private fun EmptyContentPreview() {
    BggAppTheme {
        EmptyContent(
            R.string.empty_comments,
            painterResource(R.drawable.ic_twotone_comment_48),
            Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
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
            Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}
