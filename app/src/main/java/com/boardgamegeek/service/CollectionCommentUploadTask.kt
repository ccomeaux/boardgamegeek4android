package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import okhttp3.OkHttpClient

class CollectionCommentUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override val timestampColumn = Collection.COMMENT_DIRTY_TIMESTAMP

    override val fieldName = "comment"

    override fun getValue() = collectionItem.comment.orEmpty()

    override val isDirty = collectionItem.commentTimestamp > 0
}
