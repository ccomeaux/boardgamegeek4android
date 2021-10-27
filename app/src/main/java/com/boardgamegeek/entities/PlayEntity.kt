package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

data class PlayEntity(
    val internalId: Long = BggContract.INVALID_ID.toLong(),
    val playId: Int = BggContract.INVALID_ID,
    private val rawDate: String,
    val gameId: Int,
    val gameName: String,
    val quantity: Int = 1,
    val length: Int = 0,
    val location: String = "",
    val incomplete: Boolean = false,
    val noWinStats: Boolean = false,
    val comments: String = "",
    val syncTimestamp: Long = 0L,
    private val initialPlayerCount: Int = 0,
    val dirtyTimestamp: Long = 0L,
    val updateTimestamp: Long = 0L,
    val deleteTimestamp: Long = 0L,
    val startTime: Long = 0L,
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
    val updatedPlaysTimestamp: Long = 0L,
    val subtypes: List<String> = emptyList(),
    private val _players: List<PlayPlayerEntity>? = null,
) {
    val players
        get() = _players.orEmpty()

    val isSynced
        get() = playId > 0

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

    val playerCount: Int
        get() = _players?.size ?: initialPlayerCount

    /**
     * Determine if the starting positions indicate the players are custom sorted.
     */
    fun arePlayersCustomSorted(): Boolean {
        if (players.isEmpty()) return false
        if (!hasStartingPositions()) return true
        for (i in 1..players.size) {
            val foundSeat = (getPlayerAtSeat(i) != null)
            if (!foundSeat) return true
        }
        return true
    }

    private fun hasStartingPositions(): Boolean {
        return players.all { it.startingPosition.isNotBlank() }
    }

    fun getPlayerAtSeat(seat: Int): PlayPlayerEntity? {
        return players.find { it.seat == seat }
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
            info.append(context.getString(R.string.play_description_date_segment, dateInMillis.asDate(context, includeWeekDay = true)))
        }
        if (location.isNotBlank()) info.append(context.getString(R.string.play_description_location_segment, location))
        if (length > 0) info.append(context.getString(R.string.play_description_length_segment, length.asTime()))
        if (playerCount > 0) info.append(context.resources.getQuantityString(R.plurals.play_description_players_segment, playerCount, playerCount))
        return info.trim().toString()
    }

    companion object {
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        const val UNKNOWN_DATE: Long = -1L

        fun currentDate(): String {
            return millisToRawDate(Calendar.getInstance().timeInMillis)
        }

        fun millisToRawDate(millis: Long): String {
            return FORMAT.format(millis)
        }
    }
}
