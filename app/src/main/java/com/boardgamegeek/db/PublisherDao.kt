package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PublisherDao {
    @Query("SELECT * FROM publishers")
    suspend fun loadPublishers(): List<PublisherEntity>

    @Query("SELECT publishers.*, COUNT(game_id) AS itemCount FROM publishers LEFT OUTER JOIN games_publishers ON publishers.publisher_id = games_publishers.publisher_id GROUP BY games_publishers.publisher_id")
    fun loadPublishersFlow(): Flow<List<PublisherWithItemCount>>

    @Query("SELECT * FROM publishers WHERE publisher_id=:publisherId")
    suspend fun loadPublisher(publisherId: Int): PublisherEntity?

    @Query("SELECT * FROM publishers WHERE publisher_id=:publisherId")
    fun loadPublisherFlow(publisherId: Int): Flow<PublisherEntity?>

    @Query("SELECT publishers.* FROM publishers LEFT OUTER JOIN games_publishers ON publishers.publisher_id = games_publishers.publisher_id  WHERE game_id = :gameId")
    suspend fun loadPublishersForGame(gameId: Int): List<PublisherEntity>

    @Query("UPDATE publishers SET publisher_hero_image_url=:url WHERE publisher_id=:publisherId")
    suspend fun updateHeroImageUrl(publisherId: Int, url: String)

    @Query("UPDATE publishers SET whitmore_score=:score, publisher_stats_updated_timestamp=:timestamp WHERE publisher_id=:publisherId")
    suspend fun updateWhitmoreScore(publisherId: Int, score: Int, timestamp: Date)

    @Insert(PublisherEntity::class)
    suspend fun insert(publisher: PublisherForUpsert)

    @Update(PublisherEntity::class)
    suspend fun update(publisher: PublisherForUpsert)

    @Insert(PublisherEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(publisher: PublisherBriefForUpsert)

    @Query("DELETE FROM publishers")
    suspend fun deleteAll(): Int
}