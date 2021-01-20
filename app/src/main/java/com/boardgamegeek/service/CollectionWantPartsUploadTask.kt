package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import okhttp3.OkHttpClient

class CollectionWantPartsUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override val timestampColumn = Collection.WANT_PARTS_DIRTY_TIMESTAMP

    @Suppress("SpellCheckingInspection")
    override val fieldName = "wantpartslist"

    override fun getValue() = collectionItem.wantParts.orEmpty()

    override val isDirty = collectionItem.wantPartsDirtyTimestamp > 0
}
