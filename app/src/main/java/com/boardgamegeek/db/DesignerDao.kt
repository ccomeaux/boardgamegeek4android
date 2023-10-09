package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.boardgamegeek.db.model.DesignerBasic
import com.boardgamegeek.db.model.DesignerEntity
import java.util.Date

@Dao
interface DesignerDao {
    @Query("SELECT * FROM designers")
    suspend fun loadDesigners(): List<DesignerEntity>

    @Query("SELECT * FROM designers WHERE designer_id=:designerId")
    suspend fun loadDesigner(designerId: Int): DesignerEntity?

    @Query("UPDATE designers SET designer_image_url=:imageUrl, designer_thumbnail_url=:thumbnailUrl, designer_images_updated_timestamp=:timestamp WHERE designer_id=:designerId")
    suspend fun updateImageUrls(designerId: Int, imageUrl: String, thumbnailUrl: String, timestamp: Date)

    @Query("UPDATE designers SET designer_hero_image_url=:url WHERE designer_id=:designerId")
    suspend fun updateHeroImageUrl(designerId: Int, url: String)

    @Query("UPDATE designers SET whitmore_score=:score, designer_stats_updated_timestamp=:timestamp WHERE designer_id=:designerId")
    suspend fun updateWhitmoreScore(designerId: Int, score: Int, timestamp: Date)

    @Insert(DesignerEntity::class)
    suspend fun insert(designer: DesignerBasic)

    @Update(DesignerEntity::class)
    suspend fun update(designer: DesignerBasic)

    @Query("DELETE FROM designers")
    suspend fun deleteAll(): Int
}
