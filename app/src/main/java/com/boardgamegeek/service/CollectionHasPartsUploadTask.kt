package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import okhttp3.OkHttpClient

class CollectionHasPartsUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override val timestampColumn = Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP

    @Suppress("SpellCheckingInspection")
    override val fieldName = "haspartslist"

    override fun getValue() = collectionItem.hasParts.orEmpty()

    override val isDirty: Boolean
        get() = collectionItem.hasPartsDirtyTimestamp > 0
}
