package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionWantPartsUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {

    override fun getTextColumn(): String {
        return Collection.WANTPARTS_LIST
    }

    override fun getTimestampColumn(): String {
        return Collection.WANT_PARTS_DIRTY_TIMESTAMP
    }

    override fun getFieldName(): String {
        return "wantpartslist"
    }

    override fun getValue(collectionItem: CollectionItem): String {
        return collectionItem.wantParts ?: ""
    }

    override fun isDirty(): Boolean {
        return collectionItem.wantPartsDirtyTimestamp > 0
    }
}
