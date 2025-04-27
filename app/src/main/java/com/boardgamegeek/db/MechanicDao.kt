package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.MechanicEntity
import com.boardgamegeek.db.model.MechanicWithItemCount
import kotlinx.coroutines.flow.Flow

@Dao
interface MechanicDao {
    @Query("SELECT mechanics.*, COUNT(game_id) AS itemCount FROM mechanics LEFT OUTER JOIN games_mechanics ON mechanics.mechanic_id = games_mechanics.mechanic_id GROUP BY games_mechanics.mechanic_id")
    fun loadMechanicsFLow(): Flow<List<MechanicWithItemCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mechanicEntity: MechanicEntity)

    @Query("DELETE FROM mechanics")
    suspend fun deleteAll(): Int
}
