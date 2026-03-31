package com.boardgamegeek.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.boardgamegeek.model.Game
import com.boardgamegeek.ui.compose.ComposeWebView
import com.boardgamegeek.ui.theme.BggAppTheme

@Composable
fun DescriptionScreen(game: Game, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        ComposeWebView(game.description, modifier = Modifier.fillMaxWidth())
        ThingFooter(
            game.updated,
            game.id,
            Modifier.padding(top = 8.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun DescriptionScreenPreview() {
    BggAppTheme {
        DescriptionScreen(
            testGame,
            Modifier.fillMaxWidth()
        )
    }
}

private val testGame = Game(
    id = 162886,
    name = "Spirit Island",
    thumbnailUrl = "",
    heroImageUrl = "",
    description = """
        In the most distant reaches of the world, magic still exists, embodied by spirits of the land, of the sky, and of every natural thing. As the great powers of Europe stretch their colonial empires further and further, they will inevitably lay claim to a place where spirits still hold power - and when they do, the land itself will fight back alongside the islanders who live there.

        Spirit Island is a complex and thematic co-operative game about defending your island home from colonizing Invaders. Players are different spirits of the land, each with its own unique elemental powers. Every turn, players simultaneously choose which of their power cards to play, paying energy to do so. Using combinations of power cards that match a spirit's elemental affinities can grant free bonus effects. Faster powers take effect immediately, before the Invaders spread and ravage, but other magics are slower, requiring forethought and planning to use effectively. In the Spirit phase, spirits gain energy, and choose how / whether to Grow: to reclaim used power cards, to seek new power, or to spread their presence into new areas of the island.

        The Invaders expand across the island map in a semi-predictable fashion. Each turn they explore into some lands (portions of the island); the next turn, they build in those lands, forming towns and cities. The turn after that, they ravage there, bringing blight to the land and attacking any native islanders present. The islanders fight back against the Invaders when attacked, and lend the spirits some other aid, but may not always do so exactly as you'd hoped. Some Powers work through the islanders, helping them, for example, to drive out the Invaders or clean the land of blight.

        The game escalates as it progresses: spirits spread their presence to new parts of the island and seek out new and more potent powers, while the Invaders step up their colonization efforts. Each turn represents 1-3 years of alternate history. At game start, winning requires destroying every last explorer, town and city on the board - but as you frighten the Invaders more and more, victory becomes easier: they'll run away even if explorers or even towns and cities remain. Defeat comes if any spirit is destroyed, if the island is overrun by blight, or if the Invader deck is depleted before achieving victory.

        The game includes different adversaries to fight against (eg., a Swedish Mining Colony, or a Remote British Colony). Each changes play in different ways, and offers a different path of difficulty boosts to keep the game challenging as you gain skill.
    """.trimIndent(),
)
