package com.boardgamegeek.mappers

import android.content.Context
import com.boardgamegeek.db.model.CollectionViewEntity
import com.boardgamegeek.db.model.CollectionViewFilterEntity
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.export.model.CollectionViewForExport
import com.boardgamegeek.export.model.CollectionViewFilterForExport
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sorter.CollectionSorterFactory

fun CollectionViewEntity.mapToModel(filters: List<CollectionViewFilterEntity>? = null, context: Context? = null): CollectionView {
    val factory = context?.let { CollectionFiltererFactory(it) }
    return CollectionView(
        id = id,
        name = name.orEmpty(),
        sortType = sortType ?: CollectionSorterFactory.TYPE_UNKNOWN,
        count = selectedCount ?: 0,
        timestamp = selectedTimestamp ?: 0L,
        starred = starred == true,
        filters = filters?.let { list ->
            list.mapNotNull {
                factory?.create(it.type ?: CollectionFiltererFactory.TYPE_UNKNOWN, it.data.orEmpty())
            }
        }
    )
}

fun CollectionView.mapForExport() = CollectionViewForExport(
    name = this.name,
    sortType = this.sortType,
    starred = this.starred,
    filters = this.filters?.map { CollectionViewFilterForExport(it.type, it.deflate()) }.orEmpty(),
)

fun CollectionViewForExport.mapToModel(context: Context? = null): CollectionView {
    val factory = context?.let { CollectionFiltererFactory(it) }
    return CollectionView(
        id = BggContract.INVALID_ID,
        name = name,
        sortType = sortType,
        count = 0,
        timestamp = 0L,
        starred = starred,
        filters = filters.mapNotNull { factory?.create(it.type, it.data) }
    )
}

fun CollectionView.mapToEntity() = CollectionViewEntity(
    id = if (id == BggContract.INVALID_ID) 0 else id,
    name = name,
    sortType = sortType,
    selectedCount = count,
    selectedTimestamp = timestamp,
    starred = starred,
)

fun CollectionFilterer.mapToEntity(viewId: Int) = CollectionViewFilterEntity(
    id = 0, // This works because filters are always deleted and re-crated on upsert
    viewId = viewId,
    type = type,
    data = deflate(),
)
