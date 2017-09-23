package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionTradeConditionUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {

    override fun getTextColumn(): String {
        return Collection.CONDITION
    }

    override fun getTimestampColumn(): String {
        return Collection.TRADE_CONDITION_DIRTY_TIMESTAMP
    }

    override fun getFieldName(): String {
        return "conditiontext"
    }

    override fun getValue(collectionItem: CollectionItem): String {
        return collectionItem.tradeCondition ?: ""
    }

    override fun isDirty(): Boolean {
        return collectionItem.tradeConditionDirtyTimestamp > 0
    }
}
