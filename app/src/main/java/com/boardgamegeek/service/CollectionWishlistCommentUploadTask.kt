package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionWishlistCommentUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {

    override fun getTextColumn(): String {
        return Collection.WISHLIST_COMMENT
    }

    override fun getTimestampColumn(): String {
        return Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP
    }

    override fun getFieldName(): String {
        return "wishlistcomment"
    }

    override fun getValue(collectionItem: CollectionItem): String {
        return collectionItem.wishlistComment ?: ""
    }

    override fun isDirty(): Boolean {
        return collectionItem.wishlistCommentDirtyTimestamp > 0
    }
}
