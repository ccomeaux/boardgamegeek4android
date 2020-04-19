package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionWishlistCommentUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override fun getTimestampColumn() = Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP

    override fun getFieldName() = "wishlistcomment"

    override fun getValue(collectionItem: CollectionItem) = collectionItem.wishlistComment.orEmpty()

    override fun isDirty() = collectionItem.wishlistCommentDirtyTimestamp > 0
}
