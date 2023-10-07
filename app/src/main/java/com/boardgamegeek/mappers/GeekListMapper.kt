package com.boardgamegeek.mappers

import com.boardgamegeek.entities.GeekListComment
import com.boardgamegeek.entities.GeekList
import com.boardgamegeek.entities.GeekListItem
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.*
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

fun GeekListResponse.mapToModel(): GeekList {
    val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)
    return GeekList(
        id = id,
        title = title.orEmpty().trim(),
        username = username.orEmpty(),
        description = description.orEmpty().trim(),
        numberOfItems = numitems.toIntOrNull() ?: 0,
        numberOfThumbs = thumbs.toIntOrNull() ?: 0,
        postTicks = postdate.toMillis(dateFormat),
        editTicks = editdate.toMillis(dateFormat),
        items = items?.map { it.mapToModel() }.orEmpty(),
        comments = comments.mapToModel()
    )
}

fun GeekListEntry.mapToModel(): GeekList {
    val id = if (this.href.isEmpty()) {
        BggContract.INVALID_ID
    } else {
        @Suppress("SpellCheckingInspection")
        this.href.substring(
            this.href.indexOf("/geeklist/") + 10,
            this.href.lastIndexOf("/"),
        ).toIntOrNull() ?: BggContract.INVALID_ID
    }
    return GeekList(
        id = id,
        title = this.title.orEmpty().trim(),
        username = this.username.orEmpty().trim(),
        numberOfItems = this.numitems,
        numberOfThumbs = this.numpositive
    )
}

private fun GeekListItemRemote.mapToModel(): GeekListItem {
    val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)
    return GeekListItem(
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
        comments = this.comments.mapToModel()
    )
}

private fun GeekListCommentRemote.mapToModel(): GeekListComment {
    val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)
    return GeekListComment(
        postDate = this.postdate.toMillis(dateFormat),
        editDate = this.editdate.toMillis(dateFormat),
        numberOfThumbs = this.thumbs.toIntOrNull() ?: 0,
        username = this.username.orEmpty(),
        content = this.content.orEmpty().trim()
    )
}

private fun List<GeekListCommentRemote>?.mapToModel() = this?.map { it.mapToModel() }.orEmpty()

private const val DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss Z"
