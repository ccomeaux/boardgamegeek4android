package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.PlayerColorsEntity

@Dao
interface PlayerColorDao {
    @Query("SELECT * FROM player_colors WHERE player_color_sort=1")
    suspend fun loadFavoritePlayerColors(): List<PlayerColorsEntity>

    @Query("SELECT * FROM player_colors WHERE player_type = $TYPE_USER AND player_name = :username  ORDER BY player_color_sort ASC")
    suspend fun loadColorsForUser(username: String): List<PlayerColorsEntity>

    @Query("SELECT * FROM player_colors WHERE player_type = $TYPE_PLAYER AND player_name = :playerName ORDER BY player_color_sort ASC")
    suspend fun loadColorsForPlayer(playerName: String): List<PlayerColorsEntity>

    @Transaction
    suspend fun upsertColorsForPlayer(colors: List<PlayerColorsEntity>): List<Long> {
        colors.firstOrNull()?.let {
            deleteColorsForPlayer(it.playerType, it.playerName)
        }
        return insertPlayerColors(colors)
    }

    @Insert
    suspend fun insertPlayerColors(colors: List<PlayerColorsEntity>): List<Long>

    @Query("DELETE FROM player_colors WHERE player_type = :playerType AND player_name = :playerName")
    suspend fun deleteColorsForPlayer(playerType: Int, playerName: String): Int

    companion object {
        const val TYPE_USER = 1
        const val TYPE_PLAYER = 2
    }
}