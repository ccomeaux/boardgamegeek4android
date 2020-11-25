package com.boardgamegeek.entities

import android.content.Context
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.extensions.forDatabase
import com.boardgamegeek.extensions.toMillis
import java.text.SimpleDateFormat
import java.util.*

data class PlayEntity(
        val internalId: Long,
        val playId: Int,
        private val rawDate: String,
        val gameId: Int,
        val gameName: String,
        val quantity: Int,
        val length: Int,
        val location: String,
        val incomplete: Boolean,
        val noWinStats: Boolean,
        val comments: String,
        val syncTimestamp: Long,
        val playerCount: Int,
        val dirtyTimestamp: Long = 0L,
        val updateTimestamp: Long = 0L,
        val deleteTimestamp: Long = 0L,
        val startTime: Long = 0L,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val heroImageUrl: String = "",
        val updatedPlaysTimestamp: Long = 0L
) {
    private val _players = mutableListOf<PlayPlayerEntity>()
    val players: List<PlayPlayerEntity>
        get() = _players

    val dateInMillis: Long by lazy {
        rawDate.toMillis(FORMAT, UNKNOWN_DATE)
    }

    fun dateForDisplay(context: Context): CharSequence {
        return dateInMillis.asPastDaySpan(context, includeWeekDay = true)
    }

    fun dateForDatabase(): String {
        return dateInMillis.forDatabase()
    }

    fun hasStarted(): Boolean {
        return length == 0 && startTime > 0
    }

    fun addPlayer(player: PlayPlayerEntity) {
        _players.add(player)
    }

    private fun hasStartingPositions(): Boolean {
        return _players.all { !it.startingPosition.isNullOrBlank() }
    }

    /**
     * Determine if the starting positions indicate the players are custom sorted.
     */
    fun arePlayersCustomSorted(): Boolean {
        if (_players.size == 0) return false
        if (!hasStartingPositions()) return true
        for (i in 1.._players.size) {
            val foundSeat = (_players.find { it.seat == i } != null)
            if (!foundSeat) return true
        }
        return true
    }

    fun generateSyncHashCode(): Int {
        val sb = StringBuilder()
        sb.append(dateForDatabase()).append("\n")
        sb.append(quantity).append("\n")
        sb.append(length).append("\n")
        sb.append(incomplete).append("\n")
        sb.append(noWinStats).append("\n")
        sb.append(location).append("\n")
        sb.append(comments).append("\n")
        for (player in players) {
            sb.append(player.username).append("\n")
            sb.append(player.userId).append("\n")
            sb.append(player.name).append("\n")
            sb.append(player.startingPosition).append("\n")
            sb.append(player.color).append("\n")
            sb.append(player.score).append("\n")
            sb.append(player.isNew).append("\n")
            sb.append(player.rating).append("\n")
            sb.append(player.isWin).append("\n")
        }
        return sb.toString().hashCode()
    }

    fun describe(context: Context, includeDate: Boolean = false): String {
        val info = StringBuilder()
        if (quantity > 1) info.append(context.resources.getQuantityString(R.plurals.play_description_quantity_segment, quantity, quantity))
        if (includeDate && dateInMillis != UNKNOWN_DATE) {
            val date = DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_WEEKDAY)
            info.append(context.getString(R.string.play_description_date_segment, date))
        }
        if (location.isNotBlank()) info.append(context.getString(R.string.play_description_location_segment, location))
        if (length > 0) info.append(context.getString(R.string.play_description_length_segment, length.asTime()))
        if (playerCount > 0) info.append(context.resources.getQuantityString(R.plurals.play_description_players_segment, playerCount, playerCount))
        return info.trim().toString()
    }

    companion object {
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private const val UNKNOWN_DATE: Long = -1L

        fun currentDate(): String {
            val c = Calendar.getInstance()
            return FORMAT.format(c.timeInMillis)
        }
    }
}
