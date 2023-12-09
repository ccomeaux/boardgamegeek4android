package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.boardgamegeek.db.model.*

@Dao
interface CollectionDaoNew {
    @Query("SELECT * FROM collection ORDER BY collection_sort_name COLLATE NOCASE")
    suspend fun loadAll(): List<CollectionItemWithGameEntity>

    @Query("SELECT * FROM collection WHERE _id = :internalId")
    suspend fun load(internalId: Long): CollectionItemWithGameEntity?

    @Query("SELECT * FROM collection WHERE game_id = :gameId")
    suspend fun loadForGame(gameId: Int): List<CollectionItemWithGameEntity>

    @Query("SELECT * FROM collection WHERE collection_delete_timestamp > 0")
    suspend fun loadItemsPendingDeletion(): List<CollectionItemWithGameEntity>

    @Query("SELECT * FROM collection WHERE collection_dirty_timestamp > 0 AND (collection_delete_timestamp IS NULL OR collection_delete_timestamp = '' AND collection_delete_timestamp = 0)")
    suspend fun loadItemsPendingInsert(): List<CollectionItemWithGameEntity>

    @Query("SELECT * FROM collection WHERE status_dirty_timestamp > 0 OR rating_dirty_timestamp > 0 OR comment_dirty_timestamp > 0 OR private_info_dirty_timestamp > 0 OR wishlist_comment_dirty_timestamp > 0 OR trade_condition_dirty_timestamp > 0 OR want_parts_dirty_timestamp > 0 OR has_parts_dirty_timestamp > 0")
    suspend fun loadItemsPendingUpdate(): List<CollectionItemWithGameEntity>

    @Query("SELECT acquired_from FROM collection GROUP BY acquired_from")
    suspend fun loadAcquiredFrom(): List<String>

    @Query("SELECT inventory_location FROM collection GROUP BY inventory_location")
    suspend fun loadInventoryLocation(): List<String>

    @Update(CollectionItemEntity::class)
    suspend fun updateStatuses(statuses: CollectionStatusEntity): Int

    @Update(CollectionItemEntity::class)
    suspend fun updatePrivateInfo(privateInfo: CollectionPrivateInfoEntity): Int

    @Query("UPDATE collection SET collection_hero_image_url = :url WHERE _id=:internalId")
    suspend fun updateHeroImageUrl(internalId: Long, url: String): Int

    @Query("UPDATE collection SET comment = :text, comment_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateComment(internalId: Long, text: String, timestamp: Long): Int

    @Query("UPDATE collection SET rating = :rating, rating_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateRating(internalId: Long, rating: Double, timestamp: Long): Int

    @Query("UPDATE collection SET collection_delete_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateDeletedTimestamp(internalId: Long, timestamp: Long): Int

    @Query("UPDATE collection SET private_comment = :text, private_info_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updatePrivateComment(internalId: Long, text: String, timestamp: Long): Int // TODO is this the right timestamp column?

    @Query("UPDATE collection SET wishlistcomment = :text, wishlist_comment_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateWishlistComment(internalId: Long, text: String, timestamp: Long): Int

    @Query("UPDATE collection SET conditiontext = :text, trade_condition_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateTradeCondition(internalId: Long, text: String, timestamp: Long): Int

    @Query("UPDATE collection SET haspartslist = :text, has_parts_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateHasParts(internalId: Long, text: String, timestamp: Long): Int

    @Query("UPDATE collection SET wantpartslist = :text, want_parts_dirty_timestamp = :timestamp WHERE _id=:internalId")
    suspend fun updateWantParts(internalId: Long, text: String, timestamp: Long): Int

    @Query("UPDATE collection SET collection_dirty_timestamp=0, status_dirty_timestamp=0, comment_dirty_timestamp=0, rating_dirty_timestamp=0, private_info_dirty_timestamp=0, wishlist_comment_dirty_timestamp=0, trade_condition_dirty_timestamp=0, has_parts_dirty_timestamp=0, want_parts_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearDirtyTimestamps(internalId: Long): Int

    @Query("UPDATE collection SET collection_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearItemDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET status_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearStatusDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET rating_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearRatingDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET comment_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearCommentDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET trade_condition_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearTradeConditionDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET private_info_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearPrivateInfoDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET wishlist_comment_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearWishlistCommentDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET has_parts_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearHasPartsDirtyTimestamp(internalId: Long): Int

    @Query("UPDATE collection SET want_parts_dirty_timestamp=0 WHERE _id=:internalId")
    suspend fun clearWantPartsDirtyTimestamp(internalId: Long): Int
}