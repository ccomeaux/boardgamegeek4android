package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class PublisherBriefForUpsert(
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo(name = "publisher_id")
    val publisherId: Int,
    @ColumnInfo(name = "publisher_name")
    val publisherName: String,
)
