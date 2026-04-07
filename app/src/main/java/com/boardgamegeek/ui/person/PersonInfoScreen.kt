package com.boardgamegeek.ui.person

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.Person
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.game.ThingFooter
import java.util.Date

@Composable
fun PersonInfoScreen(
    person: Person,
    type: Person.Type,
    modifier: Modifier = Modifier,
) {
    if (person.description.isBlank()) {
        EmptyContent(
            stringResource(R.string.empty_person_description, type.mapToDescription().lowercase(LocalLocale.current.platformLocale)),
            type.mapToPainter(),
            modifier = modifier.verticalScroll(rememberScrollState()),
        )
    } else {
        Column(modifier.verticalScroll(rememberScrollState())) {
            Text(
                person.description.trim(),
                modifier = Modifier.fillMaxWidth()
            )
            ThingFooter(
                syncTimestamp = person.updatedTimestamp?.time ?: 0L,
                gameId = person.id,
                Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun PersonInfoScreenView() {
    PersonInfoScreen(
        Person(
            internalId = 1L,
            id = 42,
            name = "Uwe Rosenberg",
            description = """Uwe Rosenberg (born 27 March 1970 in Aurich, Germany) is a German game designer. He has become known mainly for his card game Bohnanza, which is successful both in Germany as internationally. He also designed Agricola, a game that dethroned Puerto Rico as the highest rated game on BoardGameGeek.com.

Rosenberg first began to occupy himself with the development and mechanisms of games during his school years. During that time, he published a number of play-by-mail games, some of which are now available at www.omido.de. When he was a student, Amigo published his best-known game, Bohnanza. Since finishing his statistics studies in Dortmund (the subject of his thesis was "Probability distributions in Memory"), his main occupation is the development of games.[1]

In 2000, he founded the small publishing company Lookout Games, together with a few other authors. It published a number of expansions to Bohnanza, partly in cooperation with Hanno Girke. Larger projects were still published at other publishers, such as Amigo and Kosmos.""",
            updatedTimestamp = Date(712345678901L),
        ),
        Person.Type.Designer,
    )
}