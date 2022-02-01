package com.boardgamegeek.model

import android.content.Context
import android.os.Parcelable
import android.text.format.DateUtils
import com.boardgamegeek.extensions.forDatabase
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

        private val _players: MutableList<Player> = ArrayList(),

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

    // TEMP
    fun deepCopy(): Play {
        return this.copy(_players = mutableListOf<Player>().apply { addAll(players.map { it.copy() }) })
    }

    // DATE
    val dateForDatabase: String
        get() = dateInMillis.forDatabase()

    fun getDateForDisplay(context: Context?): CharSequence {
        return DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_WEEKDAY)
    }

    // PLAYERS
    val players: MutableList<Player>
        get() = _players

    fun getPlayerCount(): Int {
        return _players.size
    }

    fun addPlayer(player: Player) {
        // if player has seat, bump down other players
        if (!arePlayersCustomSorted() && player.seat != Player.SEAT_UNKNOWN) {
            for (i in _players.size downTo player.seat) {
                getPlayerAtSeat(i)?.seat = i + 1
            }
        }
        _players.add(player)
        _players.sortBy { player -> player.seat }
    }

    fun getPlayerAtSeat(seat: Int): Player? {
        return _players.find { it.seat == seat }
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

    // MISC

    /**
     * Determines if this play appears to have started.
     *
     * @return true, if it's not ended and the start time has been set.
     */
    fun hasStarted(): Boolean {
        return length == 0 && startTime > 0
    }

    companion object {
        const val QUANTITY_DEFAULT = 1
        const val LENGTH_DEFAULT = 0
        val FORMAT_DATABASE: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
