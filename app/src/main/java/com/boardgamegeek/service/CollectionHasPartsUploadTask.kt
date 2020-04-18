package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionHasPartsUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override fun getTimestampColumn() = Collection.HAS_PARTS_DIRTY_TIMESTAMP

    override fun getFieldName() = "haspartslist"

    override fun getValue(collectionItem: CollectionItem) = collectionItem.hasParts.orEmpty()

    override fun isDirty() = collectionItem.hasPartsDirtyTimestamp > 0
}
