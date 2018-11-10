package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.extensions.toMillis
import java.text.SimpleDateFormat
import java.util.*

data class PlayEntity(
        val internalId: Long,
        val playId: Int,
        val date: String,
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
        val dirtyTimestamp: Long,
        val startTime: Long,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val heroImageUrl: String = ""
) {
    val dateInMillis = date.toMillis(FORMAT)

    fun describe(context: Context): String {
        val info = StringBuilder()
        if (quantity > 1) info.append(context.resources.getQuantityString(R.plurals.play_description_quantity_segment, quantity, quantity))
        if (location.isNotBlank()) info.append(context.getString(R.string.play_description_location_segment, location))
        if (length > 0) info.append(context.getString(R.string.play_description_length_segment, length.asTime()))
        if (playerCount > 0) info.append(context.resources.getQuantityString(R.plurals.play_description_players_segment, playerCount, playerCount))
        return info.trim().toString()
    }

    companion object {
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
