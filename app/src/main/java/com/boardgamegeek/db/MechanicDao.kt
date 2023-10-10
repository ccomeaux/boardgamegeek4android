package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import com.boardgamegeek.db.model.MechanicEntity

@Dao
interface MechanicDao {
    @Query("SELECT * FROM mechanics")
    suspend fun loadMechanics(): List<MechanicEntity>

    @Query("DELETE FROM mechanics")
    suspend fun deleteAll(): Int
}