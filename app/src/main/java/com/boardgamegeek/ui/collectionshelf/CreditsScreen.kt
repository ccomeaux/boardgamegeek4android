package com.boardgamegeek.ui.collectionshelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.ArtistsActivity
import com.boardgamegeek.ui.CategoriesActivity
import com.boardgamegeek.ui.DesignersActivity
import com.boardgamegeek.ui.MechanicsActivity
import com.boardgamegeek.ui.PublishersActivity

@Composable
fun CreditsFlowRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement
            .spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = {
            context.startActivity<DesignersActivity>()
        }) {
            Text(stringResource(R.string.title_designers))
        }
        Button(onClick = {
            context.startActivity<ArtistsActivity>()
        }) {
            Text(stringResource(R.string.title_artists))
        }
        Button(onClick = {
            context.startActivity<PublishersActivity>()
        }) {
            Text(stringResource(R.string.title_publishers))
        }
        Button(onClick = {
            context.startActivity<MechanicsActivity>()
        }) {
            Text(stringResource(R.string.title_mechanics))
        }
        Button(onClick = {
            context.startActivity<CategoriesActivity>()
        }) {
            Text(stringResource(R.string.title_categories))
        }
    }
}

@Preview
@Composable
private fun CreditsFlowRowPreview() {
    CreditsFlowRow()
}
