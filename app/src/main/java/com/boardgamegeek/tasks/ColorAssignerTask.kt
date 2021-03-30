package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.events.ColorAssignmentCompleteEvent
import com.boardgamegeek.extensions.load
import com.boardgamegeek.extensions.queryStrings
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract.*
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.*

class ColorAssignerTask(context: Context?, private val play: Play) : AsyncTask<Void?, Void?, ColorAssignerTask.Results>() {
    @SuppressLint("StaticFieldLeak")
    private val context: Context? = context?.applicationContext

    private val colorsAvailable = mutableListOf<String>()
    private val playersNeedingColor = mutableListOf<PlayerColorChoices>()
    private val results = Results()
    private var round = 0

    override fun doInBackground(vararg params: Void?): Results {
        // set up
        val result = populatePlayersNeedingColor()
        if (result != ResultStatus.SUCCESS) {
            results.resultCode = result
            return results
        }
        if (playersNeedingColor.isEmpty()) {
            results.resultCode = ResultStatus.ERROR_NO_PLAYERS
            return results
        }
        populateColorsAvailable()
        if (colorsAvailable.size < playersNeedingColor.size) {
            results.resultCode = ResultStatus.ERROR_TOO_FEW_COLORS
            return results
        }
        populatePlayerColorChoices()

        // process
        round = 1
        var shouldContinue = true
        while (shouldContinue && playersNeedingColor.size > 0) {
            while (shouldContinue && playersNeedingColor.size > 0) {
                shouldContinue = assignTopChoice()
            }
            shouldContinue = assignMostPreferredChoice()
            round++
        }

        // assign colors to remaining players randomly
        while (playersNeedingColor.isNotEmpty()) {
            val color = colorsAvailable.random()
            val username = playersNeedingColor.random()
            assignColorToPlayer(color, username, "random")
        }
        results.resultCode = ResultStatus.SUCCESS
        return results
    }

    override fun onPostExecute(results: Results) {
        if (results.resultCode == ResultStatus.SUCCESS) {
            setPlayerColorsFromResults(results)
        }
        notifyCompletion(results, getMessageIdFromResults(results))
    }

    private fun setPlayerColorsFromResults(results: Results) {
        for (pr in this.results.results) {
            val player = getPlayerFromResult(pr)
            if (player == null) {
                results.resultCode = ResultStatus.ERROR_SOMETHING_CHANGED
                break
            } else {
                player.color = pr.color
            }
        }
    }

    private fun getMessageIdFromResults(results: Results): Int {
        return when (results.resultCode) {
            ResultStatus.SUCCESS -> R.string.msg_color_success
            ResultStatus.ERROR -> R.string.title_error
            ResultStatus.ERROR_NO_PLAYERS -> R.string.msg_color_error_no_players
            ResultStatus.ERROR_DUPLICATE_PLAYER -> R.string.msg_color_error_duplicate_player
            ResultStatus.ERROR_MISSING_PLAYER_NAME -> R.string.msg_color_error_missing_player_name
            ResultStatus.ERROR_SOMETHING_CHANGED -> R.string.msg_color_error_something_changed
            ResultStatus.ERROR_TOO_FEW_COLORS -> R.string.msg_color_error_too_few_colors
        }
    }

    private fun notifyCompletion(results: Results, @StringRes messageId: Int) {
        EventBus.getDefault().postSticky(ColorAssignmentCompleteEvent(results.resultCode == ResultStatus.SUCCESS, messageId))
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

    private fun getPlayersWithTopChoice(colorToAssign: String): List<PlayerColorChoices> {
        return playersNeedingColor.filter { it.isTopChoice(colorToAssign) }
    }

    private fun assignMostPreferredChoice(): Boolean {
        val players = mutableListOf<PlayerColorChoices>()
        var maxPreference = Double.MIN_VALUE
        for (color in colorsAvailable) {
            val playersWithTopChoice = getPlayersWithTopChoice(color)
            if (playersWithTopChoice.size > 1) {
                for (player in playersWithTopChoice) {
                    val preference = player.calculateCurrentPreferenceFor(color)
                    Timber.i("%s wants %s: %,.2f", player.name, color, preference)
                    if (preference > maxPreference) {
                        maxPreference = preference
                        players.clear()
                        players.add(player)
                    } else if (preference == maxPreference) {
                        players.add(player)
                    }
                }
            } else {
                Timber.i("Not enough players want %s", color)
            }
        }
        if (players.isEmpty()) {
            Timber.i("Nobody wants any color")
            return false
        }
        if (players.size == 1) {
            val player = players.first()
            val topChoice = player.topChoice
            if (topChoice != null) {
                assignColorToPlayer(topChoice.color, player, String.format("most preferred (%,.2f)", maxPreference))
                return true
            }
        } else {
            val player = players.random()
            val topChoice = player.topChoice
            if (topChoice != null) {
                assignColorToPlayer(topChoice.color, player, String.format("most preferred, but randomly chosen in a tie breaker (%,.2f)", maxPreference))
                return true
            }
        }
        Timber.i("Something went horribly wrong")
        return false
    }

    private fun populatePlayersNeedingColor(): ResultStatus {
        playersNeedingColor.clear()
        val users = mutableListOf<String>()
        val nonusers = mutableListOf<String>()
        play.players.filter { it.color.isEmpty() }.forEach { player ->
            if (player.username.isEmpty()) {
                if (player.name.isEmpty()) {
                    return ResultStatus.ERROR_MISSING_PLAYER_NAME
                }
                if (nonusers.contains(player.name)) {
                    return ResultStatus.ERROR_DUPLICATE_PLAYER
                }
                nonusers.add(player.name)
                playersNeedingColor.add(PlayerColorChoices(player.name, PlayerType.NON_USER))
            } else {
                if (users.contains(player.username)) {
                    return ResultStatus.ERROR_DUPLICATE_PLAYER
                }
                users.add(player.username)
                playersNeedingColor.add(PlayerColorChoices(player.username, PlayerType.USER))
            }
        }
        return ResultStatus.SUCCESS
    }

    private fun populateColorsAvailable() {
        colorsAvailable.clear()
        colorsAvailable.addAll(context?.contentResolver?.queryStrings(Games.buildColorsUri(play.gameId), GameColors.COLOR)?.filterNotNull()
                ?: emptyList())
        // remove already selected colors
        play.players.filter { it.color.isNotEmpty() }.forEach { colorsAvailable.remove(it.color) }
        // TODO add colors in query where it's not in the players' colors
    }

    /**
     * Populates the remaining players list with their color choices, limited to the colors remaining in the game.
     */
    private fun populatePlayerColorChoices() {
        playersNeedingColor.filter { it.name.isNotEmpty() }.forEach { player ->
            val uri: Uri = when (player.type) {
                PlayerType.USER -> PlayerColors.buildUserUri(player.name)
                PlayerType.NON_USER -> PlayerColors.buildPlayerUri(player.name)
            }
            val projection = arrayOf(PlayerColors.PLAYER_COLOR, PlayerColors.PLAYER_COLOR_SORT_ORDER)
            context?.contentResolver?.load(uri, projection)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val color = cursor.getString(0)
                    val sortOrder = cursor.getInt(1)
                    if (colorsAvailable.contains(color)) {
                        player.addChoice(ColorChoice(color, sortOrder))
                    }
                }
            }
        }
    }

    /**
     * Gets the player from the play based on the player result. Returns null if the player couldn't be found or their color is already set.
     */
    private fun getPlayerFromResult(pr: PlayerResult): Player? {
        for (player in play.players) {
            if (pr.type == PlayerType.USER && pr.name == player.username ||
                    pr.type == PlayerType.NON_USER && pr.name == player.name && player.username.isEmpty()) {
                if (player.color.isEmpty()) {
                    return player
                }
            }
        }
        return null
    }

    /**
     * Assign a color to a player, and remove both from the list of remaining colors and players. This can't be called
     * from a for each loop without ending the iteration.
     */
    private fun assignColorToPlayer(color: String, player: PlayerColorChoices, reason: String) {
        val playerResult = PlayerResult(player.name, player.type, color, reason)
        results.results.add(playerResult)
        Timber.i("Assigned %s", playerResult)
        colorsAvailable.remove(color)
        playersNeedingColor.remove(player)
        playersNeedingColor.forEach { it.removeChoice(color) }
    }

    inner class Results {
        var resultCode = ResultStatus.ERROR
        val results: MutableList<PlayerResult> = ArrayList()
        override fun toString() = String.format("%1\$s - %2\$s", resultCode, results.size)
    }

    inner class PlayerResult(val name: String, val type: PlayerType, val color: String, private val reason: String) {
        override fun toString() = String.format("%1\$s - %3\$s (%4\$s in round %5\$d)", name, type, color, reason, round)
    }

    private inner class PlayerColorChoices(val name: String, val type: PlayerType) {
        private val colors = mutableListOf<ColorChoice>()

        fun isTopChoice(color: String): Boolean {
            return topChoice?.color == color
        }

        /**
         * Gets the player's top remaining color choice, or `null` if they have no choices left.
         */
        val topChoice: ColorChoice?
            get() = colors.firstOrNull()

        fun addChoice(colorChoice: ColorChoice) {
            colors.add(colorChoice)
        }

        fun removeChoice(color: String): Boolean {
            return colors.removeAll { it.color == color }
        }

        fun calculateCurrentPreferenceFor(color: String): Double {
            return when (colors.size) {
                0 -> 0.0
                1 -> (100 - colors.first().sortOrder).toDouble()
                else -> {
                    val total = colors.sumBy { it.sortOrder }
                    val current = colors.find { it.color == color }?.sortOrder ?: 0

                    val expectedValue = total.toDouble() / colors.size
                    val expectedValueWithoutColor = (total - current).toDouble() / (colors.size - 1)
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

    private inner class ColorChoice(val color: String, val sortOrder: Int) {
        override fun toString() = "#$sortOrder: $color"
    }

    enum class ResultStatus {
        SUCCESS,
        ERROR,
        ERROR_NO_PLAYERS,
        ERROR_MISSING_PLAYER_NAME,
        ERROR_TOO_FEW_COLORS,
        ERROR_DUPLICATE_PLAYER,
        ERROR_SOMETHING_CHANGED,
    }

    enum class PlayerType {
        USER, NON_USER
    }
}
