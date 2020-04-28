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
                        FORMAT.parse(comment.postdate.orEmpty())?.time ?: 0L,
                        FORMAT.parse(comment.editdate.orEmpty())?.time ?: 0L,
                        comment.thumbs.toIntOrNull() ?: 0,
                        comment.username.orEmpty(),
                        comment.content.orEmpty().trim()
                )
            }

            items += GeekListItemEntity(
                    item.id.toLongOrNull() ?: BggContract.INVALID_ID.toLong(),
                    item.objectid.toIntOrNull() ?: BggContract.INVALID_ID,
                    item.objectname.orEmpty(),
                    item.objecttype.orEmpty(),
                    item.subtype.orEmpty(),
                    item.imageid.toIntOrNull() ?: 0,
                    item.username.orEmpty(),
                    item.body.orEmpty(),
                    item.thumbs.toIntOrNull() ?: 0,
                    FORMAT.parse(item.postdate.orEmpty())?.time ?: 0L,
                    FORMAT.parse(item.editdate.orEmpty())?.time ?: 0L,
                    comments
            )
        }
        val comments = mutableListOf<GeekListCommentEntity>()
        from.comments?.forEach { comment ->
            comments += GeekListCommentEntity(
                    FORMAT.parse(comment.postdate.orEmpty())?.time ?: 0L,
                    FORMAT.parse(comment.editdate.orEmpty())?.time ?: 0L,
                    comment.thumbs.toIntOrNull() ?: 0,
                    comment.username.orEmpty(),
                    comment.content.orEmpty().trim()
            )
        }

        return GeekListEntity(
                from.id,
                from.title.orEmpty().trim(),
                from.username.orEmpty(),
                from.description.orEmpty().trim(),
                from.numitems.toIntOrNull() ?: 0,
                from.thumbs.toIntOrNull() ?: 0,
                FORMAT.parse(from.postdate.orEmpty())?.time ?: 0L,
                FORMAT.parse(from.editdate.orEmpty())?.time ?: 0L,
                items,
                comments
        )
    }

    companion object {
        private val FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    }
}
