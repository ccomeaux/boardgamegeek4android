package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract.Collection
import okhttp3.OkHttpClient

class CollectionTradeConditionUploadTask(client: OkHttpClient) : CollectionTextUploadTask(client) {
    override val timestampColumn = Collection.TRADE_CONDITION_DIRTY_TIMESTAMP

    @Suppress("SpellCheckingInspection")
    override val fieldName = "conditiontext"

    override fun getValue() = collectionItem.tradeCondition.orEmpty()

    override val isDirty: Boolean
        get() = collectionItem.tradeConditionDirtyTimestamp > 0
}
