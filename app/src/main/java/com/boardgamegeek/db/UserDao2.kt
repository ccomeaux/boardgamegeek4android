package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import com.boardgamegeek.db.model.UserEntity

@Dao
interface UserDao2 {
    @Query("SELECT * FROM users WHERE username=:username")
    fun loadUser(username: String): UserEntity?
}
