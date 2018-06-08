package com.boardgamegeek.entities

import android.text.TextUtils
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class PlayEntity(
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
        val syncTimestamp: Long
) {
    val dateInMillis = tryParseDate(date)
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