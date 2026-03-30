package com.boardgamegeek.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.compose.ListItemAvatar
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.ListItemThumbnail
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun GameDetailListItem(
    detail: GameDetail,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(ListItemDefaults.oneLineHeight)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(detail.name)
    }
}

@Composable
fun GameDetailPersonListItem(
    person: GameDetail,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(ListItemDefaults.threeLineHeight)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemAvatar(person.thumbnailUrl)
        Column(verticalArrangement = Arrangement.Center) {
            ListItemPrimaryText(person.name)
            if (person.description.isNotBlank())
                ListItemSecondaryText(person.description, maxLines = 2)
        }
    }
}

@Composable
fun GameDetailThingListItem(
    thing: GameDetail,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(ListItemDefaults.threeLineHeight)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemThumbnail(thing.thumbnailUrl)
        Column(verticalArrangement = Arrangement.Center) {
            ListItemPrimaryText(thing.name)
            if (thing.description.isNotBlank())
                ListItemSecondaryText(thing.description, maxLines = 2)
        }
    }
}

@Preview
@Composable
private fun GameDetailListItemPreview() {
    BggAppTheme {
        GameDetailListItem(
            GameDetail(
                id = 2819,
                name = "Solo / Solitaire Game",
                description = """Games that are intended for play by a single player, or that have a game mode intended for play by a single player.

This does not include games in which a player plays both (or multiple) factions in play to the best of his abilities (something that often happens to Wargame players or to Chess players).""",
            )
        )
    }
}

@Preview
@Composable
private fun GameDetailPersonListItemPreview() {
    BggAppTheme {
        GameDetailPersonListItem(
            GameDetail(
                id = 10,
                name = "Uwe Rosenberg",
                description = """Uwe Rosenberg (born 27 March 1970 in Aurich, Germany) is a German game designer. He has become known mainly for his card game Bohnanza, which is successful both in Germany as internationally. He also designed Agricola, a game that dethroned Puerto Rico as the highest rated game on BoardGameGeek.com.

Rosenberg first began to occupy himself with the development and mechanisms of games during his school years. During that time, he published a number of play-by-mail games, some of which are now available at www.omido.de. When he was a student, Amigo published his best-known game, Bohnanza. Since finishing his statistics studies in Dortmund (the subject of his thesis was "Probability distributions in Memory"), his main occupation is the development of games.[1]

In 2000, he founded the small publishing company Lookout Games, together with a few other authors. It published a number of expansions to Bohnanza, partly in cooperation with Hanno Girke. Larger projects were still published at other publishers, such as Amigo and Kosmos.

Rosenberg is well known for the development of innovative card game mechanisms. Another main point of his work are the research-intensive games, that have peculiar historical events as their theme, and games that deal with clichés about men and women. Since 2005, he focusses on complex building games with an economic theme: his first, Agricola, was released in October 2007. As second game in this series, Le Havre was published in October 2008.

He married Susanne Balders on 18 May 2007. He lives in Gütersloh and works at his studio in Dortmund.""",
            )
        )
    }
}

@Preview
@Composable
private fun GameDetailThingListItemPreview() {
    BggAppTheme {
        GameDetailThingListItem(
            GameDetail(
                id = 29156,
                name = "Mindclash Games",
                description = """Formed in 2014, Mindclash Games is a nine-man army of board game design, illustration and publishing. Our flagship project in the works is currently Voidfall, with more to follow soon!""",
            )
        )
    }
}