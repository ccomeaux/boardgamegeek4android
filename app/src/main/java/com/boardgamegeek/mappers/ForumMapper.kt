package com.boardgamegeek.mappers

import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.Forum
import java.text.SimpleDateFormat
import java.util.*

class ForumMapper {
    fun map(from: Forum): ForumEntity {
        return ForumEntity(
                id = from.id,
                title = from.title,
                numberOfThreads = from.numthreads,
                lastPostDateTime = from.lastpostdate.toMillis(dateFormat),
                isHeader = from.noposting == 1
        )
    }

    companion object {
        private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    }
}
