package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.model.CollectionItem

import okhttp3.OkHttpClient

class CollectionTradeConditionUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override fun getTimestampColumn() = Collection.TRADE_CONDITION_DIRTY_TIMESTAMP

    override fun getFieldName() = "conditiontext"

    override fun getValue(collectionItem: CollectionItem) = collectionItem.tradeCondition.orEmpty()

    override fun isDirty() = collectionItem.tradeConditionDirtyTimestamp > 0
}
