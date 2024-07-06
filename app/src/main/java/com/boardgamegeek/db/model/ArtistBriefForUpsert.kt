package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class ArtistBriefForUpsert (
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "artist_id")
    val artistId: Int,
    @ColumnInfo(name = "artist_name")
    val artistName: String,
)
