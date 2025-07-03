package com.boardgamegeek.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.model.Company
import com.boardgamegeek.ui.theme.BggAppTheme
import java.util.Date

@Composable
fun CompanyListItem(company: Company, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .then(modifier)
    ) {
        CompanyImage(
            company,
            modifier = Modifier
                .padding(end = 16.dp)
                .align(Alignment.Top)
        )
        Column {
            Text(
                company.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pluralStringResource(R.plurals.games_suffix, company.itemCount, company.itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.whitmore_score_prefix, company.whitmoreScore),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompanyImage(company: Company, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(company.thumbnailUrl)
            .crossfade(true)
            .build(),
        placeholder = painterResource(R.drawable.thumbnail_image_empty),
        error = painterResource(R.drawable.thumbnail_image_empty),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(56.dp).clip(MaterialTheme.shapes.extraSmall)
    )
}

@PreviewLightDark
@Composable
fun CompanyListItemPreview(
    @PreviewParameter(CompanyPreviewParameterProvider::class) company: Company
) {
    BggAppTheme {
        CompanyListItem(
            company,
            Modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                    vertical = dimensionResource(R.dimen.material_margin_vertical),
                )
        )
    }
}

class CompanyPreviewParameterProvider : PreviewParameterProvider<Company> {
    override val values = sequenceOf(
        Company(
            internalId = 42L,
            id = 1,
            name = "Fantasy Flight Games",
            sortName = "Fantasy Flight Games",
            description = "A dude!",
            updatedTimestamp = null,
            thumbnailUrl = "",
            imageUrl = "",
            heroImageUrl = "",
            itemCount = 42,
            whitmoreScore = 17,
            statsUpdatedTimestamp = Date(System.currentTimeMillis()),
        ),
        Company(
            internalId = 43L,
            id = 2,
            name = "Capstone Games",
            sortName = "Capstone Games",
            description = "A dude!",
            updatedTimestamp = Date(12345678901L),
            thumbnailUrl = "https://cf.geekdo-images.com/yaDyQ_rrLU7P4HUm1ebX5Q__imagepagezoom/img/ryZWb1v-qW3gMLnXE1J6uxOc3LM=/fit-in/1200x900/filters:no_upscale():strip_icc()/pic1452960.jpg",
            imageUrl = "",
            heroImageUrl = "",
            itemCount = 1,
            whitmoreScore = 0,
            statsUpdatedTimestamp = Date(System.currentTimeMillis()),
        ),
    )
}
