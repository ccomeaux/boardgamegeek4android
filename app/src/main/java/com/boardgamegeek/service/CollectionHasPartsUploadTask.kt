package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionHasPartsUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {

    override fun getTextColumn(): String {
        return Collection.HASPARTS_LIST
    }

    override fun getTimestampColumn(): String {
        return Collection.HAS_PARTS_DIRTY_TIMESTAMP
    }

    override fun getFieldName(): String {
        return "haspartslist"
    }

    override fun getValue(collectionItem: CollectionItem): String {
        return collectionItem.hasParts ?: ""
    }

    override fun isDirty(): Boolean {
        return collectionItem.hasPartsDirtyTimestamp > 0
    }
}
