package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionCommentUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override fun getTimestampColumn() = Collection.COMMENT_DIRTY_TIMESTAMP

    override fun getFieldName() = "comment"

    override fun getValue(collectionItem: CollectionItem) = collectionItem.comment.orEmpty()

    override fun isDirty() = collectionItem.commentTimestamp > 0
}
