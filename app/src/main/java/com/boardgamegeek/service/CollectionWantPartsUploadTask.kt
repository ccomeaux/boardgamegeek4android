package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionWantPartsUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override fun getTimestampColumn() = Collection.WANT_PARTS_DIRTY_TIMESTAMP

    override fun getFieldName() = "wantpartslist"

    override fun getValue(collectionItem: CollectionItem) = collectionItem.wantParts.orEmpty()

    override fun isDirty() = collectionItem.wantPartsDirtyTimestamp > 0
}
