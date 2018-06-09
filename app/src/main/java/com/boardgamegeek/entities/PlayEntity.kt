package com.boardgamegeek.entities

import android.content.Context
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.asTime
import timber.log.Timber
import java.text.DateFormat
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
        val playerCount: Int
) {
    val dateInMillis = tryParseDate(date)

    fun describe(context: Context): String {
        val info = StringBuilder()
        if (quantity > 1) info.append(context.resources.getQuantityString(R.plurals.play_description_quantity_segment, quantity, quantity))
        if (location.isNotBlank()) info.append(context.getString(R.string.play_description_location_segment, location))
        if (length > 0) info.append(context.getString(R.string.play_description_length_segment, length.asTime()))
        if (playerCount > 0) info.append(context.resources.getQuantityString(R.plurals.play_description_players_segment, playerCount, playerCount))
        return info.trim().toString()
    }

}

private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun tryParseDate(date: String, format: DateFormat = FORMAT): Long {
    return if (TextUtils.isEmpty(date)) {
        0L
    } else {
        try {
            format.parse(date).time
        } catch (e: Exception) {
            Timber.w(e, "Unable to parse %s as %s", date, format)
            0L
        }
    }
}