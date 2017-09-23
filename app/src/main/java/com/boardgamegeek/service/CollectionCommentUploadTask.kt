package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionCommentUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {

    override fun getTextColumn(): String {
        return Collection.COMMENT
    }

    override fun getTimestampColumn(): String {
        return Collection.COMMENT_DIRTY_TIMESTAMP
    }

    override fun getFieldName(): String {
        return "comment"
    }

    override fun getValue(collectionItem: CollectionItem): String {
        return collectionItem.comment ?: ""
    }

    override fun isDirty(): Boolean {
        return collectionItem.commentTimestamp > 0
    }
}
