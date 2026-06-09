package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.toFormattedString

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    infoText: String? = null,
    count: Int? = null,
) {
    Row(
        modifier = modifier
            .heightIn(48.dp)
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text + if (count != null) " - ${count.toFormattedString()}" else "",
            style = MaterialTheme.typography.titleLarge,
        )
        infoText?.let {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.End),
                tooltip = {
                    PlainTooltip { Text(infoText) }
                },
                state = rememberTooltipState(isPersistent = true)
            ) {
                Icon(
                    painter = painterResource(R.drawable.help_24px),
                    contentDescription = stringResource(R.string.help_title),
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    val modifier = Modifier.padding(horizontal = 16.dp)
    Column {
        SectionHeader("Recently Viewed", modifier)
        SectionHeader("Friendless Favorites", modifier, count = 10)
        SectionHeader("Hidden Gems", modifier, "information")
    }
}
