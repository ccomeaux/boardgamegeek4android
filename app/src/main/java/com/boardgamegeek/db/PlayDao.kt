package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayDao {
    @Transaction
    suspend fun upsert(play: PlayEntity, players: List<PlayPlayerEntity>): Long {
        val internalId = if (play.internalId == 0L) {
            insertPlay(play)
        } else {
            updatePlay(play)
            deletePlayers(play.internalId)
            play.internalId
        }
        insertPlayers(players.map { it.copy(internalPlayId = internalId) })
        return internalId
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlay(play: PlayEntity): Long

    @Update
    suspend fun updatePlay(play: PlayEntity)

    @Query("DELETE FROM play_players WHERE _play_id = :playInternalId")
    suspend fun deletePlayers(playInternalId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayPlayerEntity>)

    @Query("SELECT * FROM plays ORDER BY date DESC, play_id DESC")
    suspend fun loadPlays(): List<PlayEntity>

    @Query("SELECT * FROM plays ORDER BY date DESC, play_id DESC")
    fun loadPlaysFlow(): Flow<List<PlayEntity>>

    @Query("SELECT * FROM plays WHERE play_id = :playId")
    suspend fun loadPlay(playId: Int): PlayEntity?

    @Query("SELECT * FROM plays WHERE object_id = :gameId ORDER BY date DESC, play_id DESC")
    suspend fun loadPlaysForGame(gameId: Int): List<PlayEntity>

    @Query("SELECT * FROM plays WHERE object_id = :gameId ORDER BY date DESC, play_id DESC")
    fun loadPlaysForGameFlow(gameId: Int): Flow<List<PlayEntity>>

    @Query("SELECT * FROM plays WHERE location = :location ORDER BY date DESC, play_id DESC")
    suspend fun loadPlaysForLocation(location: String): List<PlayEntity>

    @Query("SELECT * FROM plays WHERE location = :location ORDER BY date DESC, play_id DESC")
    fun loadPlaysForLocationFlow(location: String): Flow<List<PlayEntity>>

    @Query("SELECT plays.* FROM plays LEFT OUTER JOIN play_players ON plays._id = play_players._play_id WHERE user_name = :username ORDER BY date DESC, play_id DESC")
    fun loadPlaysForUserFlow(username: String): Flow<List<PlayEntity>>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE user_name = :username AND (delete_timestamp=0 OR delete_timestamp IS NULL)")
    suspend fun loadPlayersForUser(username: String): List<PlayerWithPlayEntity>

    @Query("SELECT plays.* FROM plays LEFT OUTER JOIN play_players ON plays._id = play_players._play_id WHERE user_name = :username AND object_id = :gameId ORDER BY date DESC, play_id DESC")
    suspend fun loadPlaysForUserAndGame(username: String, gameId: Int): List<PlayEntity>

    @Query("SELECT plays.* FROM plays LEFT OUTER JOIN play_players ON plays._id = play_players._play_id WHERE name = :name AND (user_name = '' OR user_name IS NULL) ORDER BY date DESC, play_id DESC")
    fun loadPlaysForPlayerFlow(name: String): Flow<List<PlayEntity>>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE name = :name AND (user_name = '' OR user_name IS NULL)")
    suspend fun loadPlayersForPlayer(name: String): List<PlayerWithPlayEntity>

    @Query("SELECT plays.* FROM plays LEFT OUTER JOIN play_players ON plays._id = play_players._play_id WHERE name = :name AND object_id = :gameId AND (user_name = '' OR user_name IS NULL) ORDER BY date DESC, play_id DESC")
    suspend fun loadPlaysForPlayerAndGame(name: String, gameId: Int): List<PlayEntity>

    @Transaction
    @Query("SELECT * FROM plays WHERE update_timestamp>0 ORDER BY date DESC, play_id DESC")
    suspend fun loadUpdatingPlays(): List<PlayWithPlayersEntity>

    @Transaction
    @Query("SELECT * FROM plays WHERE delete_timestamp>0 ORDER BY date DESC, play_id DESC")
    suspend fun loadDeletingPlays(): List<PlayWithPlayersEntity>

    @Transaction
    @Query("SELECT plays.*, games.image_url AS gameImageUrl, games.thumbnail_url As gameThumbnailUrl, games.hero_image_url AS gameHeroImageUrl FROM plays LEFT JOIN games ON games.game_id = plays.object_id WHERE plays._id = :internalId")
    suspend fun loadPlayWithPlayers(internalId: Long): PlayWithPlayersAndImagesEntity?

    @Transaction
    @Query("SELECT plays.*, games.image_url AS gameImageUrl, games.thumbnail_url As gameThumbnailUrl, games.hero_image_url AS gameHeroImageUrl FROM plays LEFT JOIN games ON games.game_id = plays.object_id WHERE plays._id = :internalId")
    fun loadPlayWithPlayersFlow(internalId: Long): Flow<PlayWithPlayersAndImagesEntity?>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE object_id = :gameId AND (delete_timestamp=0 OR delete_timestamp IS NULL)")
    suspend fun loadPlayersForGame(gameId: Int): List<PlayerWithPlayEntity>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE object_id = :gameId AND (delete_timestamp=0 OR delete_timestamp IS NULL)")
    fun loadPlayersForGameFlow(gameId: Int): Flow<List<PlayerWithPlayEntity>>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE delete_timestamp=0 OR delete_timestamp IS NULL")
    suspend fun loadPlayers(): List<PlayerWithPlayEntity>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE delete_timestamp=0 OR delete_timestamp IS NULL")
    fun loadPlayersFlow(): Flow<List<PlayerWithPlayEntity>>

    @Query("SELECT play_players.*, plays.quantity, plays.no_win_stats AS noWinStats, plays.incomplete, users.avatar_url AS avatarUrl FROM play_players JOIN plays ON plays._id = play_players._play_id LEFT JOIN users ON users.username = play_players.user_name WHERE location = :location AND (delete_timestamp=0 OR delete_timestamp IS NULL)")
    suspend fun loadPlayersForLocation(location: String): List<PlayerWithPlayEntity>

    @Query("SELECT location AS name, SUM(quantity) AS playCount from plays WHERE delete_timestamp IS NULL OR delete_timestamp = 0 GROUP BY location ORDER BY playCount DESC")
    suspend fun loadLocations(): List<LocationEntity>

    @Query("SELECT location AS name, SUM(quantity) AS playCount from plays WHERE delete_timestamp IS NULL OR delete_timestamp = 0 GROUP BY location ORDER BY playCount DESC")
    fun loadLocationsFlow(): Flow<List<LocationEntity>>

    @Transaction
    suspend fun updateUsername(playInternalId: Long, playerInternalId: Long, username: String) {
        updatePlayerUsername(playerInternalId, username)
        markAsUpdated(playInternalId)
    }

    @Transaction
    suspend fun updateNickname(playInternalId: Long, playerInternalId: Long, nickname: String) {
        updatePlayerNickname(playerInternalId, nickname)
        markAsUpdated(playInternalId)
    }

    @Query("UPDATE play_players SET user_name = :username WHERE _id = :internalId")
    suspend fun updatePlayerUsername(internalId: Long, username: String)

    @Query("UPDATE play_players SET name = :nickname WHERE _id = :internalId")
    suspend fun updatePlayerNickname(internalId: Long, nickname: String)

    @Query("UPDATE plays SET update_timestamp = :timestamp, dirty_timestamp = 0, delete_timestamp = 0 WHERE _id = :internalId")
    suspend fun markAsUpdated(internalId: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE plays SET delete_timestamp = :timestamp, update_timestamp = 0, dirty_timestamp = 0 WHERE _id = :internalId")
    suspend fun markAsDeleted(internalId: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE plays SET delete_timestamp = 0, update_timestamp = 0, dirty_timestamp = 0 WHERE _id = :internalId")
    suspend fun markAsDiscarded(internalId: Long): Int

    @Query("UPDATE plays SET play_id = :playId, delete_timestamp = 0, update_timestamp = 0, dirty_timestamp = 0 WHERE _id = :internalId")
    suspend fun markAsSynced(internalId: Long, playId: Int): Int

    @Query("UPDATE plays SET updated_list = :timestamp WHERE _id = :internalId")
    suspend fun updateSyncTimestamp(internalId: Long, timestamp: Long): Int

    @Query("UPDATE plays SET sync_hash_code = 0")
    suspend fun clearSyncHashCodes()

    @Query("UPDATE plays SET location = :location WHERE _id = :internalId")
    suspend fun updateLocation(internalId: Long, location: String): Int

    @Query("UPDATE plays SET location = :location, update_timestamp = :timestamp WHERE _id = :internalId")
    suspend fun updateLocation(internalId: Long, location: String, timestamp: Long): Int

    @Query("DELETE FROM plays")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM plays WHERE _id = :internalId")
    suspend fun delete(internalId: Long): Int

    @Query("DELETE FROM plays WHERE object_id = :gameId AND updated_list < :syncTimestamp AND (update_timestamp = '' OR update_timestamp IS NULL) AND (delete_timestamp = '' OR delete_timestamp IS NULL) AND (dirty_timestamp = '' OR dirty_timestamp IS NULL)")
    suspend fun deleteUnupdatedPlaysForGame(gameId: Int, syncTimestamp: Long): Int

    @Query("DELETE FROM plays WHERE date <= :date AND updated_list < :syncTimestamp AND (update_timestamp = '' OR update_timestamp IS NULL) AND (delete_timestamp = '' OR delete_timestamp IS NULL) AND (dirty_timestamp = '' OR dirty_timestamp IS NULL)")
    suspend fun deleteUnupdatedPlaysBeforeDate(date: String, syncTimestamp: Long): Int

    @Query("DELETE FROM plays WHERE date >= :date AND updated_list < :syncTimestamp AND (update_timestamp = '' OR update_timestamp IS NULL) AND (delete_timestamp = '' OR delete_timestamp IS NULL) AND (dirty_timestamp = '' OR dirty_timestamp IS NULL)")
    suspend fun deleteUnupdatedPlaysAfterDate(date: String, syncTimestamp: Long): Int
}
