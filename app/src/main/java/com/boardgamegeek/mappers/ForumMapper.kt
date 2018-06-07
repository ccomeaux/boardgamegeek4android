package com.boardgamegeek.mappers

import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.io.model.Forum
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private val FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

class ForumMapper {
    fun map(from: Forum): ForumEntity {
        return ForumEntity(
                id = from.id,
                title = from.title,
                numberOfThreads = from.numthreads,
                lastPostDateTime = tryParseDate(from.lastpostdate),
                isHeader = from.noposting == 1
        )
    }

    private fun tryParseDate(date: String, format: DateFormat = FORMAT): Long {
        return if (date.isBlank()) {
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
}