package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface DesignerDao {
    @Query("SELECT * FROM designers")
    suspend fun loadDesigners(): List<DesignerEntity>

    @Query("SELECT designers.*, COUNT(game_id) AS itemCount FROM designers LEFT OUTER JOIN games_designers ON designers.designer_id = games_designers.designer_id GROUP BY games_designers.designer_id")
    fun loadDesignersFlow(): Flow<List<DesignerWithItemCount>>

    @Query("SELECT * FROM designers WHERE designer_id=:designerId")
    suspend fun loadDesigner(designerId: Int): DesignerEntity?

    @Query("SELECT * FROM designers WHERE designer_id=:designerId")
    fun loadDesignerFlow(designerId: Int): Flow<DesignerEntity?>

    @Query("UPDATE designers SET designer_image_url=:imageUrl, designer_thumbnail_url=:thumbnailUrl, designer_images_updated_timestamp=:timestamp WHERE designer_id=:designerId")
    suspend fun updateImageUrls(designerId: Int, imageUrl: String, thumbnailUrl: String, timestamp: Date)

    @Query("UPDATE designers SET designer_hero_image_url=:url WHERE designer_id=:designerId")
    suspend fun updateHeroImageUrl(designerId: Int, url: String)

    @Query("UPDATE designers SET whitmore_score=:score, designer_stats_updated_timestamp=:timestamp WHERE designer_id=:designerId")
    suspend fun updateWhitmoreScore(designerId: Int, score: Int, timestamp: Date)

    @Insert(DesignerEntity::class)
    suspend fun insert(designer: DesignerForUpsert)

    @Update(DesignerEntity::class)
    suspend fun update(designer: DesignerForUpsert)

    @Insert(DesignerEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(artist: DesignerBriefForUpsert)

    @Query("DELETE FROM designers")
    suspend fun deleteAll(): Int
}
