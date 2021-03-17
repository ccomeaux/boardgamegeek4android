package com.boardgamegeek.model

import android.content.Context
import android.os.Parcelable
import android.text.format.DateUtils
import com.boardgamegeek.extensions.forDatabase
import com.boardgamegeek.extensions.howManyMinutesOld
import com.boardgamegeek.provider.BggContract.INVALID_ID
import kotlinx.android.parcel.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Play @JvmOverloads constructor(
        @JvmField
        var gameId: Int = INVALID_ID,
        @JvmField
        var gameName: String = "",
        @JvmField
        var dateInMillis: Long = Calendar.getInstance().timeInMillis,
        @JvmField
        var quantity: Int = QUANTITY_DEFAULT,
        @JvmField
        var length: Int = LENGTH_DEFAULT,
        @JvmField
        var location: String? = "",
        @JvmField
        var comments: String? = "",

        private var _players: MutableList<Player> = ArrayList(),

        @JvmField
        var playId: Int = INVALID_ID,
        @JvmField
        var incomplete: Boolean = false,
        @JvmField
        var noWinStats: Boolean = false,
        @JvmField
        var startTime: Long = 0L,

        @JvmField
        var syncTimestamp: Long = 0L,
        @JvmField
        var deleteTimestamp: Long = 0L,
        @JvmField
        var updateTimestamp: Long = 0L,
        @JvmField
        var dirtyTimestamp: Long = 0L,
) : Parcelable {

    @JvmField
    var subtypes: List<String>? = null

    val isSynced
        get() = playId > 0

    // DATE
    val dateForDatabase: String
        get() = dateInMillis.forDatabase()

    fun getDateForDisplay(context: Context?): CharSequence {
        return DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_WEEKDAY)
    }

    fun setDate(year: Int, month: Int, day: Int) {
        val c = Calendar.getInstance()
        c[Calendar.DAY_OF_MONTH] = day
        c[Calendar.MONTH] = month
        c[Calendar.YEAR] = year
        dateInMillis = c.timeInMillis
    }

    // PLAYERS
    val players: List<Player>
        get() = _players

    fun getPlayerCount(): Int {
        return _players.size
    }

    fun setPlayers(players: List<Player>) {
        _players.addAll(players)
    }

    fun clearPlayers() {
        _players.clear()
    }

    fun addPlayer(player: Player) {
        // if player has seat, bump down other players
        if (!arePlayersCustomSorted() && player.seat != Player.SEAT_UNKNOWN) {
            for (i in _players.size downTo player.seat) {
                getPlayerAtSeat(i)?.seat = i + 1
            }
        }
        _players.add(player)
        sortPlayers()
    }

    fun removePlayer(player: Player, resort: Boolean) {
        if (_players.size == 0) return
        if (resort && !arePlayersCustomSorted()) {
            for (i in player.seat until _players.size) {
                getPlayerAtSeat(i + 1)?.seat = i
            }
        }
        _players.remove(player)
    }

    /**
     * Replaces a player at the position with a new player. If the position doesn't exists, the player is added instead.
     */
    fun replaceOrAddPlayer(player: Player, position: Int) {
        if (position in _players.indices) {
            _players[position] = player
        } else {
            _players.add(player)
        }
    }

    fun getPlayerAtSeat(seat: Int): Player? {
        return _players.find { it.seat == seat }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int): Boolean {
        if (_players.size == 0) return false
        if (arePlayersCustomSorted()) return false
        val player = getPlayerAtSeat(fromSeat) ?: return false
        player.seat = Player.SEAT_UNKNOWN
        if (fromSeat > toSeat) {
            for (i in fromSeat - 1 downTo toSeat) {
                getPlayerAtSeat(i)?.seat = i + 1
            }
        } else {
            for (i in fromSeat + 1..toSeat) {
                getPlayerAtSeat(i)?.seat = i - 1
            }
        }
        player.seat = toSeat
        sortPlayers()
        return true
    }

    /**
     * Sets the start player based on the index, keeping the other players in order, assigns seats, then sorts
     *
     * @param startPlayerIndex The zero-based index of the new start player
     */
    fun pickStartPlayer(startPlayerIndex: Int) {
        val playerCount = _players.size
        for (i in 0 until playerCount) {
            _players[i].seat = (i - startPlayerIndex + playerCount) % playerCount + 1
        }
        sortPlayers()
    }

    /**
     * Randomizes the order of players, assigning seats to the new order.
     */
    fun randomizePlayerOrder() {
        if (_players.size == 0) return
        _players.shuffle()
        val playerCount = _players.size
        for (i in 0 until playerCount) {
            _players[i].seat = i + 1
        }
    }

    /**
     * Sort the players by seat; unseated players left unsorted at the bottom of the list.
     */
    fun sortPlayers() {
        _players.sortBy { it.seat } // TODO - I think this leaves unsorted at the top
    }

    /**
     * Determine if the starting positions indicate the players are custom sorted.
     */
    fun arePlayersCustomSorted(): Boolean {
        for (seat in 1.._players.size) {
            if (_players.count { it.seat == seat } != 1) return true
        }
        return false
    }

    /**
     * Determine if any player has a starting position.
     */
    fun hasStartingPositions(): Boolean {
        return _players.any { it.startingPosition.isNotBlank() }
    }

    /**
     * Remove the starting position for all players.
     */
    fun clearPlayerPositions() {
        _players.forEach { it.startingPosition = "" }
    }

    // MISC
    /**
     * Determine if any player has a team/color.
     */
    fun hasColors(): Boolean {
        return _players.any { it.color.isNotBlank() }
    }

    val highScore: Double
        get() = _players.maxOf { it.score.toDoubleOrNull() ?: -Double.MAX_VALUE }

    /**
     * Determines if this play appears to have started.
     *
     * @return true, if it's not ended and the start time has been set.
     */
    fun hasStarted(): Boolean {
        return length == 0 && startTime > 0
    }

    fun start() {
        length = 0
        startTime = System.currentTimeMillis()
    }

    fun resume() {
        startTime = System.currentTimeMillis() - length * DateUtils.MINUTE_IN_MILLIS
        length = 0
    }

    fun end() {
        length = if (startTime > 0) {
            startTime.howManyMinutesOld()
        } else {
            0
        }
        startTime = 0
    }

    companion object {
        const val QUANTITY_DEFAULT = 1
        const val LENGTH_DEFAULT = 0
        val FORMAT_DATABASE: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
