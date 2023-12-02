package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.MechanicEntity

@Dao
interface MechanicDao {
    @Query("SELECT * FROM mechanics")
    suspend fun loadMechanics(): List<MechanicEntity>

    @Query("SELECT * FROM mechanics WHERE mechanic_id = :id")
    suspend fun loadMechanic(id: Int): MechanicEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mechanicEntity: MechanicEntity)

    @Query("DELETE FROM mechanics")
    suspend fun deleteAll(): Int
}