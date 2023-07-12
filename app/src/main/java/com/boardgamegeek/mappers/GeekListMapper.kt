package com.boardgamegeek.mappers

import com.boardgamegeek.entities.GeekListCommentEntity
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.*
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

fun GeekListsResponse.mapToEntity() = this.lists.map { it.mapToEntity() }

fun GeekListResponse.mapToEntity(): GeekListEntity {
    val dateFormat = SimpleDateFormat(datePattern, Locale.US)
    return GeekListEntity(
        id = id,
        title = title.orEmpty().trim(),
        username = username.orEmpty(),
        description = description.orEmpty().trim(),
        numberOfItems = numitems.toIntOrNull() ?: 0,
        numberOfThumbs = thumbs.toIntOrNull() ?: 0,
        postTicks = postdate.toMillis(dateFormat),
        editTicks = editdate.toMillis(dateFormat),
        items = items?.map { it.mapToEntity() }.orEmpty(),
        comments = comments.mapToEntity()
    )
}

fun GeekListEntry.mapToEntity(): GeekListEntity {
    val id = if (this.href.isEmpty()) {
        BggContract.INVALID_ID
    } else {
        this.href.substring(
                this.href.indexOf("/geeklist/") + 10,
                this.href.lastIndexOf("/"),
        ).toIntOrNull() ?: BggContract.INVALID_ID
    }
    return GeekListEntity(
            id = id,
            title = this.title.orEmpty().trim(),
            username = this.username.orEmpty().trim(),
            numberOfItems = this.numitems,
            numberOfThumbs = this.numpositive
    )
}

private fun GeekListItem.mapToEntity(): GeekListItemEntity {
    val dateFormat = SimpleDateFormat(datePattern, Locale.US)
    return GeekListItemEntity(
        id = this.id.toLongOrNull() ?: BggContract.INVALID_ID.toLong(),
        objectId = this.objectid.toIntOrNull() ?: BggContract.INVALID_ID,
        objectName = this.objectname.orEmpty(),
        objectType = this.objecttype.orEmpty(),
        subtype = this.subtype.orEmpty(),
        imageId = this.imageid.toIntOrNull() ?: 0,
        username = this.username.orEmpty(),
        body = this.body.orEmpty(),
        numberOfThumbs = this.thumbs.toIntOrNull() ?: 0,
        postDateTime = this.postdate.toMillis(dateFormat),
        editDateTime = this.editdate.toMillis(dateFormat),
        comments = this.comments.mapToEntity()
    )
}

private fun GeekListComment.mapToEntity(): GeekListCommentEntity {
    val dateFormat = SimpleDateFormat(datePattern, Locale.US)
    return GeekListCommentEntity(
        postDate = this.postdate.toMillis(dateFormat),
        editDate = this.editdate.toMillis(dateFormat),
        numberOfThumbs = this.thumbs.toIntOrNull() ?: 0,
        username = this.username.orEmpty(),
        content = this.content.orEmpty().trim()
    )
}

private fun List<GeekListComment>?.mapToEntity() = this?.map { it.mapToEntity() }.orEmpty()

private const val datePattern = "EEE, dd MMM yyyy HH:mm:ss Z"
