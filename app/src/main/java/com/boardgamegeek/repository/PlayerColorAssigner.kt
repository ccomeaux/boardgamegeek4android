package com.boardgamegeek.repository

import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.entities.PlayerColorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayerColorAssigner(
    private val gameId: Int,
    private val players: List<PlayPlayerEntity>,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
) {
    private val colorsAvailable = mutableListOf<String>()
    private val playersNeedingColor = mutableListOf<PlayerColorChoices>()
    private val results = mutableListOf<PlayerResult>()
    private var round = 0

    suspend fun execute(): List<PlayerResult> = withContext(Dispatchers.Default) {
        // set up
        colorsAvailable.clear()
        colorsAvailable += gameRepository.getPlayColors(gameId) - players.map { it.color }.filter { it.isNotBlank() }.toSet()

        playersNeedingColor.clear()
        playersNeedingColor += players.filter { it.color.isBlank() && it.username.isNotBlank() }.distinctBy { it.username }
            .map { player ->
                PlayerColorChoices(
                    player.username,
                    PlayerType.USER,
                    playRepository.loadUserColors(player.name).filter { colorsAvailable.contains(it.description) })
            }
        playersNeedingColor += players.filter { it.color.isBlank() && it.username.isBlank() && it.name.isNotBlank() }.distinctBy { it.name }
            .map { player ->
                PlayerColorChoices(
                    player.name,
                    PlayerType.NON_USER,
                    playRepository.loadNonUserColors(player.name).filter { colorsAvailable.contains(it.description) })
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
                Timber.d("No players want %s as their top choice", colorToAssign)
                null
            }
            players.size > 1 -> {
                Timber.d("Multiple players want %s as their top choice", colorToAssign)
                null
            }
            else -> players.first()
        }
    }

    private fun assignMostPreferredChoice(): Boolean {
        val playerChoiceScores = mutableListOf<Pair<PlayerColorChoices, Double>>()
        for (color in colorsAvailable) {
            val playersWithTopChoice = getPlayersWithTopChoice(color)
            if (playersWithTopChoice.isEmpty()) {
                Timber.d("Nobody wants %s", color)
            } else {
                playerChoiceScores += playersWithTopChoice.map { it to it.calculateCurrentPreferenceFor(color) }
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
        return playersNeedingColor.filter { it.topChoice?.description == colorToAssign }
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

    private inner class PlayerColorChoices(
        val name: String,
        val type: PlayerType,
        initialColors: List<PlayerColorEntity>,
    ) {
        private val colors: MutableList<PlayerColorEntity> = initialColors.toMutableList()

        /**
         * Gets the player's top remaining color choice, or `null` if they have no choices left.
         */
        val topChoice: PlayerColorEntity?
            get() = colors.minByOrNull { it.sortOrder }

        fun removeChoice(color: String) = colors.removeAll { it.description == color }

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
