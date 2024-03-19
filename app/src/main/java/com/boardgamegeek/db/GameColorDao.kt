package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.GameColorsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameColorDao {
    @Query("SELECT * FROM game_colors")
    suspend fun loadColors(): List<GameColorsEntity>

    @Query("SELECT * FROM game_colors WHERE game_id = :gameId")
    suspend fun loadColorsForGame(gameId: Int): List<GameColorsEntity>

    @Query("SELECT * FROM game_colors WHERE game_id = :gameId")
    fun loadColorsForGameFlow(gameId: Int): Flow<List<GameColorsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(colors: List<GameColorsEntity>): List<Long>

    @Query("DELETE FROM game_colors WHERE game_id = :gameId")
    suspend fun deleteColorsForGame(gameId: Int): Int

    @Query("DELETE FROM game_colors WHERE game_id = :gameId AND color = :color")
    suspend fun deleteColorForGame(gameId: Int, color: String): Int
}
