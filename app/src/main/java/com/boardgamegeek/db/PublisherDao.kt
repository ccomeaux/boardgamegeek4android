package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.boardgamegeek.db.model.PublisherEntity
import com.boardgamegeek.db.model.PublisherForUpsert
import java.util.Date

@Dao
interface PublisherDao {
    @Query("SELECT * FROM publishers")
    suspend fun loadPublishers(): List<PublisherEntity>

    @Query("SELECT * FROM publishers WHERE publisher_id=:publisherId")
    suspend fun loadPublisher(publisherId: Int): PublisherEntity?

    @Query("UPDATE publishers SET publisher_hero_image_url=:url WHERE publisher_id=:publisherId")
    suspend fun updateHeroImageUrl(publisherId: Int, url: String)

    @Query("UPDATE publishers SET whitmore_score=:score, publisher_stats_updated_timestamp=:timestamp WHERE publisher_id=:publisherId")
    suspend fun updateWhitmoreScore(publisherId: Int, score: Int, timestamp: Date)

    @Insert(PublisherEntity::class)
    suspend fun insert(publisher: PublisherForUpsert)

    @Update(PublisherEntity::class)
    suspend fun update(publisher: PublisherForUpsert)

    @Query("DELETE FROM publishers")
    suspend fun deleteAll(): Int
}