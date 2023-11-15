package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.boardgamegeek.db.model.MechanicEntity

@Dao
interface MechanicDao {
    @Query("SELECT * FROM mechanics")
    suspend fun loadMechanics(): List<MechanicEntity>

    @Query("SELECT * FROM mechanics WHERE mechanic_id = :id")
    suspend fun loadMechanic(id: Int): MechanicEntity?

    @Upsert
    suspend fun upsert(mechanicEntity: MechanicEntity)

    @Query("DELETE FROM mechanics")
    suspend fun deleteAll(): Int
}