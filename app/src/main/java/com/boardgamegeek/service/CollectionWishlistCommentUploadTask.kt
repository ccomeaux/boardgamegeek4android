package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import okhttp3.OkHttpClient

class CollectionWishlistCommentUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override val timestampColumn = Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP

    @Suppress("SpellCheckingInspection")
    override val fieldName = "wishlistcomment"

    override fun getValue() = collectionItem.wishlistComment.orEmpty()

    override val isDirty = collectionItem.wishlistCommentDirtyTimestamp > 0
}
