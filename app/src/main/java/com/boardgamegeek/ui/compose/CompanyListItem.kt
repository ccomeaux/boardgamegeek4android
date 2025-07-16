package com.boardgamegeek.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.boardgamegeek.R
import com.boardgamegeek.model.Company
import com.boardgamegeek.ui.theme.BggAppTheme
import java.util.Date

@Composable
fun CompanyListItem(company: Company, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemThumbnail(company.thumbnailUrl)
        Column {
            ListItemPrimaryText(company.name)
            ListItemSecondaryText(pluralStringResource(R.plurals.games_suffix, company.itemCount, company.itemCount))
            ListItemSecondaryText(stringResource(R.string.whitmore_score_prefix, company.whitmoreScore))
        }
    }
}

@PreviewLightDark
@Composable
private fun CompanyListItemPreview(
    @PreviewParameter(CompanyPreviewParameterProvider::class) company: Company
) {
    BggAppTheme {
        CompanyListItem(
            company
        )
    }
}

private class CompanyPreviewParameterProvider : PreviewParameterProvider<Company> {
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
