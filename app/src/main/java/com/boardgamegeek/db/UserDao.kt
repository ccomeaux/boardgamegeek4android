package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.db.model.UserEntity
import com.boardgamegeek.db.model.UserForUpsert

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username=:username")
    suspend fun loadUser(username: String): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun loadUsers(): List<UserEntity>

    @Insert(entity = UserEntity::class)
    suspend fun insert(user: UserForUpsert)

    @Update(entity = UserEntity::class)
    suspend fun update(user: UserForUpsert)

    @Upsert(entity = UserEntity::class)
    suspend fun upsert(user: UserAsBuddyForUpsert)

    @Query("UPDATE users SET updated_detail_timestamp=:timestamp WHERE username=:username")
    suspend fun updateTimestamp(username: String, timestamp: Long)

    @Query("UPDATE users SET play_nickname=:nickname WHERE username=:username")
    suspend fun updateNickname(username: String, nickname: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM users WHERE buddy_flag=1 AND updated_list_timestamp<:timestamp")
    suspend fun deleteBuddiesAsOf(timestamp: Long): Int
}
