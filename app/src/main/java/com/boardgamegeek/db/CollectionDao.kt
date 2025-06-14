package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Transaction
    @Query("SELECT collection.*, MAX(plays.date) AS lastPlayedDate FROM collection LEFT OUTER JOIN plays ON collection.game_id = plays.object_id GROUP BY collection_id ORDER BY collection_sort_name COLLATE NOCASE")
    fun loadAll(): List<CollectionItemWithGameAndLastPlayedEntity>

    @Transaction
    @Query("SELECT collection.*, MAX(plays.date) AS lastPlayedDate FROM collection LEFT OUTER JOIN plays ON collection.game_id = plays.object_id GROUP BY collection_id ORDER BY collection_sort_name COLLATE NOCASE")
    fun loadAllAsFlow(): Flow<List<CollectionItemWithGameAndLastPlayedEntity>>

    @Transaction
    @Query("SELECT * FROM collection WHERE _id = :internalId")
    fun load(internalId: Long): CollectionItemWithGameEntity?

    @Transaction
    @Query("SELECT * FROM collection WHERE _id = :internalId")
    fun loadFlow(internalId: Long): Flow<CollectionItemWithGameEntity?>

    @Transaction
    @Query("SELECT * FROM collection WHERE collection_id = :collectionId")
    suspend fun load(collectionId: Int): CollectionItemWithGameEntity?

    @Transaction
    @Query("SELECT * FROM collection WHERE game_id = :gameId")
    suspend fun loadForGame(gameId: Int): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT * FROM collection WHERE game_id = :gameId")
    fun loadForGameFlow(gameId: Int): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT collection.* FROM games_artists INNER JOIN collection ON games_artists.game_id = collection.game_id WHERE artist_id = :artistId")
    suspend fun loadForArtist(artistId: Int): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT collection.* FROM games_artists INNER JOIN collection ON games_artists.game_id = collection.game_id WHERE artist_id = :artistId")
    fun loadForArtistAsLiveData(artistId: Int): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT collection.* FROM games_designers INNER JOIN collection ON games_designers.game_id = collection.game_id WHERE designer_id = :designerId")
    suspend fun loadForDesigner(designerId: Int): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT collection.* FROM games_designers INNER JOIN collection ON games_designers.game_id = collection.game_id WHERE designer_id = :designerId")
    fun loadForDesignerAsLiveData(designerId: Int): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT collection.* FROM games_publishers INNER JOIN collection ON games_publishers.game_id = collection.game_id WHERE publisher_id = :publisherId")
    suspend fun loadForPublisher(publisherId: Int): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT collection.* FROM games_publishers INNER JOIN collection ON games_publishers.game_id = collection.game_id WHERE publisher_id = :publisherId")
    fun loadForPublisherFlow(publisherId: Int): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT collection.* FROM games_categories INNER JOIN collection ON games_categories.game_id = collection.game_id WHERE category_id = :categoryId")
    fun loadForCategoryFlow(categoryId: Int): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT collection.* FROM games_mechanics INNER JOIN collection ON games_mechanics.game_id = collection.game_id WHERE mechanic_id = :mechanicId")
    fun loadForMechanicFlow(mechanicId: Int): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT * FROM collection WHERE collection_delete_timestamp > 0")
    suspend fun loadItemsPendingDeletion(): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT * FROM collection WHERE collection_dirty_timestamp > 0 AND (collection_delete_timestamp IS NULL OR collection_delete_timestamp = '' AND collection_delete_timestamp = 0)")
    suspend fun loadItemsPendingInsert(): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT * FROM collection WHERE status_dirty_timestamp > 0 OR rating_dirty_timestamp > 0 OR comment_dirty_timestamp > 0 OR private_info_dirty_timestamp > 0 OR wishlist_comment_dirty_timestamp > 0 OR trade_condition_dirty_timestamp > 0 OR want_parts_dirty_timestamp > 0 OR has_parts_dirty_timestamp > 0")
    suspend fun loadItemsPendingUpdate(): List<CollectionItemWithGameEntity>

    @Transaction
    @Query("SELECT * FROM collection WHERE collection_delete_timestamp > 0 OR collection_dirty_timestamp > 0 OR  status_dirty_timestamp > 0 OR rating_dirty_timestamp > 0 OR comment_dirty_timestamp > 0 OR private_info_dirty_timestamp > 0 OR wishlist_comment_dirty_timestamp > 0 OR trade_condition_dirty_timestamp > 0 OR want_parts_dirty_timestamp > 0 OR has_parts_dirty_timestamp > 0")
    fun loadItemsPendingUploadAsFlow(): Flow<List<CollectionItemWithGameEntity>>

    @Transaction
    @Query("SELECT * FROM collection WHERE updated = 0 OR updated IS NULL ORDER BY updated_list ASC")
    suspend fun loadItemsNotUpdated(): List<CollectionItemWithGameEntity>

    @Query("SELECT acquired_from FROM collection GROUP BY acquired_from")
    suspend fun loadAcquiredFrom(): List<String>

    @Query("SELECT inventory_location FROM collection GROUP BY inventory_location")
    suspend fun loadInventoryLocation(): List<String>

    @Insert(CollectionItemEntity::class)
    suspend fun insert(entity: CollectionItemForInsert): Long

    @Update(CollectionItemEntity::class)
    suspend fun update(entity: CollectionItemForUpdate): Int

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
    suspend fun updatePrivateComment(internalId: Long, text: String, timestamp: Long): Int

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

    @Query("DELETE FROM collection WHERE _id = :internalId")
    suspend fun delete(internalId: Long): Int

    @Query("DELETE FROM collection WHERE updated_list < :timestamp")
    suspend fun deleteUnupdatedItems(timestamp: Long): Int
}
