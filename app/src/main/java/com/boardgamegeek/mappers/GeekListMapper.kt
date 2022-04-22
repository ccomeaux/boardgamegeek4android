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

fun GeekListResponse.mapToEntity() = GeekListEntity(
        id = id,
        title = title.orEmpty().trim(),
        username = username.orEmpty(),
        description = description.orEmpty().trim(),
        numberOfItems = numitems.toIntOrNull() ?: 0,
        numberOfThumbs = thumbs.toIntOrNull() ?: 0,
        postTicks = postdate.toMillis(FORMAT),
        editTicks = editdate.toMillis(FORMAT),
        items = items?.map { it.mapToEntity() }.orEmpty(),
        comments = comments.mapToEntity()
)

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

private fun GeekListItem.mapToEntity() = GeekListItemEntity(
        id = this.id.toLongOrNull() ?: BggContract.INVALID_ID.toLong(),
        objectId = this.objectid.toIntOrNull() ?: BggContract.INVALID_ID,
        objectName = this.objectname.orEmpty(),
        objectType = this.objecttype.orEmpty(),
        subtype = this.subtype.orEmpty(),
        imageId = this.imageid.toIntOrNull() ?: 0,
        username = this.username.orEmpty(),
        body = this.body.orEmpty(),
        numberOfThumbs = this.thumbs.toIntOrNull() ?: 0,
        postDateTime = this.postdate.toMillis(FORMAT),
        editDateTime = this.editdate.toMillis(FORMAT),
        comments = this.comments.mapToEntity()
)

private fun GeekListComment.mapToEntity() = GeekListCommentEntity(
        postDate = this.postdate.toMillis(FORMAT),
        editDate = this.editdate.toMillis(FORMAT),
        numberOfThumbs = this.thumbs.toIntOrNull() ?: 0,
        username = this.username.orEmpty(),
        content = this.content.orEmpty().trim()
)

private fun List<GeekListComment>?.mapToEntity() = this?.map { it.mapToEntity() }.orEmpty()

private val FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
