package com.boardgamegeek.mappers

import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.io.model.Forum
import com.boardgamegeek.toMillis
import java.text.SimpleDateFormat
import java.util.*

private val FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

class ForumMapper {
    fun map(from: Forum): ForumEntity {
        return ForumEntity(
                id = from.id,
                title = from.title,
                numberOfThreads = from.numthreads,
                lastPostDateTime = from.lastpostdate.toMillis(FORMAT),
                isHeader = from.noposting == 1
        )
    }
}
