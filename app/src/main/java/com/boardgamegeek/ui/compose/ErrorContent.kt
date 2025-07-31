package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun ErrorContent(text: String, iconPainter: Painter, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(
            horizontal = dimensionResource(R.dimen.material_margin_horizontal),
            vertical = dimensionResource(R.dimen.material_margin_vertical)
        )
    ) {
        OutlinedCard(
            modifier = Modifier
                .heightIn(min = 160.dp, max = 320.dp)
                .widthIn(min = 160.dp, max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 20.dp),
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(108.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun ErrorContent(text: String, imageVector: ImageVector, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(
            horizontal = dimensionResource(R.dimen.material_margin_horizontal),
            vertical = dimensionResource(R.dimen.material_margin_vertical)
        )
    ) {
        OutlinedCard(
            modifier = Modifier
                .heightIn(min = 160.dp, max = 320.dp)
                .widthIn(min = 160.dp, max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(108.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 640, heightDp = 640)
@Composable
private fun ErrorContentPreview() {
    BggAppTheme {
        ErrorContent(
            "A long error message. I mean a really long one. I mean a really, really long one.",
            painterResource(R.drawable.ic_twotone_comment_48),
            Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 640, heightDp = 640)
@Composable
private fun ErrorContentImageVector() {
    BggAppTheme {
        ErrorContent(
            "A long error message. I mean a really long one. I mean a really, really long one.",
            Icons.Filled.Password,
            Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .heightIn(min = 160.dp, max = 320.dp)
                .widthIn(min = 160.dp, max = 320.dp)
        )
    }
}
