package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import java.util.Date

data class ArtistForUpsert (
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "artist_id")
    val artistId: Int,
    @ColumnInfo(name = "artist_name")
    val artistName: String,
    @ColumnInfo(name = "artist_description")
    val artistDescription: String?,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Date?,
)
