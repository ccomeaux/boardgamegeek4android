package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.CollectionViewEntity
import com.boardgamegeek.db.model.CollectionViewFilterEntity
import com.boardgamegeek.db.model.CollectionViewWithFilters
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionViewDao {
    @Query("SELECT * FROM collection_filters ORDER BY selected_count DESC, selected_timestamp DESC")
    suspend fun loadViewsWithoutFilters(): List<CollectionViewEntity>

    @Query("SELECT * FROM collection_filters ORDER BY selected_count DESC, selected_timestamp DESC")
    fun loadViewsWithoutFiltersFlow(): Flow<List<CollectionViewEntity>>

    @Query("SELECT * FROM collection_filters LEFT OUTER JOIN collection_filters_details ON collection_filters._id = collection_filters_details.filter_id  ORDER BY selected_count DESC, selected_timestamp DESC")
    suspend fun loadViews(): Map<CollectionViewEntity, List<CollectionViewFilterEntity>>

    @Query("SELECT * FROM collection_filters WHERE _id = :id")
    suspend fun loadView(id: Int): CollectionViewEntity?

    @Transaction
    @Query("SELECT * FROM collection_filters WHERE _id = :id")
    fun loadViewFlow(id: Int): Flow<CollectionViewWithFilters?>

    @Transaction
    suspend fun insert(view: CollectionViewEntity, filters: List<CollectionViewFilterEntity>): Long {
        val id = insert(view)
        insert(filters.map { it.copy(viewId = id.toInt()) })
        return id
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(view: CollectionViewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filters: List<CollectionViewFilterEntity>)

    @Transaction
    suspend fun update(view: CollectionViewEntity, filters: List<CollectionViewFilterEntity>) {
        update(view)
        deleteFilters(view.id) // TODO delete only unused filters and update re-used filters
        insert(filters)
    }

    @Update
    suspend fun update(view: CollectionViewEntity)

    @Query("UPDATE collection_filters SET selected_count=:selectedCount, selected_timestamp=:selectedTimestamp WHERE _id=:id")
    suspend fun updateShortcut(id: Int, selectedCount: Int, selectedTimestamp: Long)

    @Query("DELETE FROM collection_filters WHERE _id = :viewId")
    suspend fun delete(viewId: Int): Int

    @Query("DELETE FROM collection_filters_details WHERE filter_id = :viewId")
    suspend fun deleteFilters(viewId: Int)

    @Query("DELETE FROM collection_filters")
    suspend fun deleteAll(): Int
}