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
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.Person
import com.boardgamegeek.ui.theme.BggAppTheme
import java.util.Date

@Composable
fun PersonListItem(person: Person, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(ListItemTokens.paddingValues)
            .clickable(onClick = onClick)
    ) {
        ListItemAvatar(person.thumbnailUrl)
        Column {
            ListItemPrimaryText(person.name)
            ListItemSecondaryText(pluralStringResource(R.plurals.games_suffix, person.itemCount, person.itemCount))
            ListItemSecondaryText(stringResource(R.string.whitmore_score_prefix, person.whitmoreScore))
        }
    }
}

@PreviewLightDark
@Composable
private fun PersonListItemPreview(
    @PreviewParameter(PersonPreviewParameterProvider::class) person: Person
) {
    BggAppTheme {
        PersonListItem(person)
    }
}

private class PersonPreviewParameterProvider : PreviewParameterProvider<Person> {
    override val values = sequenceOf(
        Person(
            internalId = 42L,
            id = 1,
            name = "Chris Comeaux",
            description = "A dude!",
            updatedTimestamp = null,
            thumbnailUrl = "",
            itemCount = 42,
            whitmoreScore = 17,
        ),
        Person(
            internalId = 43L,
            id = 2,
            name = "Stefan Feld",
            description = "A dude!",
            updatedTimestamp = Date(12345678901L),
            thumbnailUrl = "https://cf.geekdo-images.com/yaDyQ_rrLU7P4HUm1ebX5Q__imagepagezoom/img/ryZWb1v-qW3gMLnXE1J6uxOc3LM=/fit-in/1200x900/filters:no_upscale():strip_icc()/pic1452960.jpg",
            itemCount = 1,
            whitmoreScore = 0,
        ),
    )
}
