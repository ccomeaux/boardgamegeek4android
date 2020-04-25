package com.boardgamegeek.mappers

import com.boardgamegeek.entities.GeekListCommentEntity
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.io.model.GeekListResponse
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

class GeekListMapper {
    fun map(from: GeekListResponse): GeekListEntity {
        val items = mutableListOf<GeekListItemEntity>()
        from.items?.forEach { item ->
            val comments = mutableListOf<GeekListCommentEntity>()
            item.comments?.forEach { comment ->
                comments += GeekListCommentEntity(
                        FORMAT.parse(comment.postdate).time,
                        FORMAT.parse(comment.editdate).time,
                        comment.thumbs.toIntOrNull() ?: 0,
                        comment.username,
                        comment.content.trim()
                )
            }

            items += GeekListItemEntity(
                    item.id.toLongOrNull() ?: BggContract.INVALID_ID.toLong(),
                    item.objectid.toIntOrNull() ?: BggContract.INVALID_ID,
                    item.objectname,
                    item.objecttype,
                    item.subtype,
                    item.imageid.toIntOrNull() ?: 0,
                    item.username,
                    item.body,
                    item.thumbs.toIntOrNull() ?: 0,
                    FORMAT.parse(item.postdate).time,
                    FORMAT.parse(item.editdate).time,
                    comments
            )
        }
        return GeekListEntity(
                from.id,
                from.title.orEmpty().trim(),
                from.username,
                from.description,
                from.numitems.toIntOrNull() ?: 0,
                from.thumbs.toIntOrNull() ?: 0,
                FORMAT.parse(from.postdate).time,
                FORMAT.parse(from.editdate).time,
                items
        )
    }

    companion object {
        private val FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    }
}
