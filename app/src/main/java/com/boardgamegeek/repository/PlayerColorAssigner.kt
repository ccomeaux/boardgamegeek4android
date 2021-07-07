package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.extensions.queryStrings
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayerColorAssigner(private val application: BggApplication, private val play: Play) {
    private val colorsAvailable = mutableListOf<String>()
    private val playersNeedingColor = mutableListOf<PlayerColorChoices>()
    private val results = mutableListOf<PlayerResult>()
    private var round = 0
    private val dao = PlayDao(application)

    suspend fun execute(): List<PlayerResult> = withContext(Dispatchers.Default) {
        // set up
        colorsAvailable.clear()
        val gameColors = (application.contentResolver?.queryStrings(BggContract.Games.buildColorsUri(play.gameId), BggContract.GameColors.COLOR)?.filterNotNull()
                ?: emptyList())
        val takenColors = play.players.filter { it.color.isNotEmpty() }.map { it.color }
        colorsAvailable.addAll(gameColors - takenColors)

        playersNeedingColor.clear()
        play.players.filter { it.color.isEmpty() && it.username.isNotBlank() }.distinctBy { it.username }.forEach { player ->
            playersNeedingColor.add(PlayerColorChoices(player.username, PlayerType.USER))
        }
        play.players.filter { it.color.isEmpty() && it.username.isBlank() && it.name.isNotBlank() }.distinctBy { it.name }.forEach { player ->
            playersNeedingColor.add(PlayerColorChoices(player.name, PlayerType.NON_USER))
        }

        playersNeedingColor.forEach { player ->
            val colors = when (player.type) {
                PlayerType.USER -> dao.loadUserColors(player.name)
                PlayerType.NON_USER -> dao.loadPlayerColors(player.name)
            }
            player.setColors(colors.filter { colorsAvailable.contains(it.description) })
        }

        // process
        round = 1
        var shouldContinue = true
        while (shouldContinue && playersNeedingColor.isNotEmpty() && colorsAvailable.isNotEmpty()) {
            while (shouldContinue && playersNeedingColor.isNotEmpty() && colorsAvailable.isNotEmpty()) {
                shouldContinue = assignTopChoice()
            }
            shouldContinue = assignMostPreferredChoice()
            round++
        }

        // assign colors to remaining players randomly
        while (playersNeedingColor.isNotEmpty() && colorsAvailable.isNotEmpty()) {
            val color = colorsAvailable.random()
            val username = playersNeedingColor.random()
            assignColorToPlayer(color, username, "random")
        }
        results
    }

    /**
     * Assigns a player their top color choice if no one else has that top choice as well.
     *
     * @return `true` if a color was assigned, `false` if not.
     */
    private fun assignTopChoice(): Boolean {
        for (colorToAssign in colorsAvailable) {
            val playerWhoWantsThisColor = getLonePlayerWithTopChoice(colorToAssign)
            if (playerWhoWantsThisColor != null) {
                assignColorToPlayer(colorToAssign, playerWhoWantsThisColor, "top choice")
                return true
            }
        }
        Timber.i("No more players have a unique top choice in round %d", round)
        return false
    }

    private fun getLonePlayerWithTopChoice(colorToAssign: String): PlayerColorChoices? {
        val players = getPlayersWithTopChoice(colorToAssign)
        return when {
            players.isEmpty() -> {
                Timber.i("No players want %s as their top choice", colorToAssign)
                null
            }
            players.size > 1 -> {
                Timber.i("Multiple players want %s as their top choice", colorToAssign)
                null
            }
            else -> players.first()
        }
    }

    private fun assignMostPreferredChoice(): Boolean {
        val playerChoiceScores = mutableListOf<Pair<PlayerColorChoices, Double>>()
        for (color in colorsAvailable) {
            val playersWithTopChoice = getPlayersWithTopChoice(color)
            if (playersWithTopChoice.size > 1) {
                for (player in playersWithTopChoice) {
                    val preference = player.calculateCurrentPreferenceFor(color)
                    playerChoiceScores.add(player to preference)
                    Timber.d("%s wants %s most (%,.2f)", player.playerName, color, preference)
                }
            } else {
                Timber.d("Not enough players want %s", color)
            }
        }
        if (playerChoiceScores.isEmpty()) {
            Timber.i("Nobody wants any color")
            return false
        }
        val playerChoice = playerChoiceScores.maxByOrNull { it.second }
        val topChoice = playerChoice?.first?.topChoice
        if (topChoice != null) {
            assignColorToPlayer(topChoice.description, playerChoice.first, String.format("most preferred (%,.2f)", playerChoice.second))
            return true
        }
        Timber.d("Something went horribly wrong")
        return false
    }

    private fun getPlayersWithTopChoice(colorToAssign: String): List<PlayerColorChoices> {
        return playersNeedingColor.filter { it.isTopChoice(colorToAssign) }
    }

    /**
     * Assign a color to a player, and remove both from the list of remaining colors and players. This can't be called
     * from a loop without ending the iteration.
     */
    private fun assignColorToPlayer(color: String, player: PlayerColorChoices, reason: String) {
        val playerResult = PlayerResult(player.name, player.type, color, reason)
        results.add(playerResult)
        Timber.i("Assigned %s", playerResult)
        colorsAvailable.remove(color)
        playersNeedingColor.remove(player)
        playersNeedingColor.forEach { it.removeChoice(color) }
    }

    inner class PlayerResult(val name: String, val type: PlayerType, val color: String, private val reason: String) {
        override fun toString() = "$color to $name ($type) ($reason in round $round)"
    }

    private inner class PlayerColorChoices(val name: String, val type: PlayerType) {
        private val colors = mutableListOf<PlayerColorEntity>()

        fun isTopChoice(color: String): Boolean {
            return topChoice?.description == color
        }

        /**
         * Gets the player's top remaining color choice, or `null` if they have no choices left.
         */
        val topChoice: PlayerColorEntity?
            get() = colors.minByOrNull { it.sortOrder }

        fun setColors(colorChoice: List<PlayerColorEntity>) {
            colors.clear()
            colors.addAll(colorChoice)
        }

        fun removeChoice(color: String): Boolean {
            return colors.removeAll { it.description == color }
        }

        fun calculateCurrentPreferenceFor(color: String): Double {
            return when (colors.size) {
                0 -> 0.0
                1 -> (100 - colors.first().sortOrder).toDouble()
                else -> {
                    val expectedValue = colors.map { it.sortOrder }.average()
                    val expectedValueWithoutColor = colors.filter { it.description != color }.map { it.sortOrder }.average()
                    expectedValueWithoutColor - expectedValue
                }
            }
        }

        val playerName: String
            get() = "$name ($type)"

        override fun toString(): String {
            return "$name ($type)" + if (colors.size > 0) {
                colors.joinToString(", ", " [", "]")
            } else ""
        }
    }

    enum class PlayerType {
        USER, NON_USER
    }
}
