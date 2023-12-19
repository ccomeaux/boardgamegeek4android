package com.boardgamegeek.model

import android.content.Context
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract

data class Play(
    val internalId: Long = BggContract.INVALID_ID.toLong(),
    val playId: Int = BggContract.INVALID_ID,
    val dateInMillis: Long,
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
    val gameIsCustomSorted: Boolean = false,
    val subtypes: List<String> = emptyList(),
    private val _players: List<PlayPlayer>? = null,
) {
    val players
        get() = _players.orEmpty()

    val isSynced
        get() = playId > 0

    val heroImageUrls = listOf(heroImageUrl, thumbnailUrl, imageUrl).filter { it.isNotBlank() }

    val robustHeroImageUrl: String
        get() = heroImageUrl.ifBlank { thumbnailUrl.ifBlank { imageUrl } }

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
        for (seat in 1..players.size) {
            if (players.count { it.seat == seat } != 1) return true
        }
        return false
    }

    fun getPlayerAtSeat(seat: Int): PlayPlayer? {
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
        players.forEach { player ->
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
            info.append(
                context.getString(
                    R.string.play_description_date_segment,
                    dateInMillis.formatDateTime(
                        context,
                        flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_WEEKDAY
                    )
                )
            )
        }
        if (location.isNotBlank()) info.append(context.getString(R.string.play_description_location_segment, location))
        if (length > 0) info.append(context.getString(R.string.play_description_length_segment, length.asTime()))
        if (playerCount > 0) info.append(context.resources.getQuantityString(R.plurals.play_description_players_segment, playerCount, playerCount))
        return info.trim().toString()
    }

    companion object {
        const val UNKNOWN_DATE: Long = -1L
    }
}
