package com.boardgamegeek.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun ListHeader(headerText: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
    ) {
        Text(
            headerText,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.recycler_section_header_height))
                .wrapContentSize(align = Alignment.Center)
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = 0.dp
                )
        )
    }
}

@PreviewLightDark
@Composable
private fun ListHeaderPreview() {
    BggAppTheme {
        ListHeader(
            "100+",
            Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}
