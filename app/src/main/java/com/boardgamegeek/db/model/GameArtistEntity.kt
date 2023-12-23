package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "games_artists",
    foreignKeys = [
        ForeignKey(GameEntity::class, ["game_id"], ["game_id"], ForeignKey.CASCADE),
        ForeignKey(ArtistEntity::class, ["artist_id"], ["artist_id"]),
    ],
)
data class GameArtistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "game_id", index = true)
    val gameId: Int,
    @ColumnInfo(name = "artist_id", index = true)
    val artistId: Int,
)