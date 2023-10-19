package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CollectionViewEntity
import com.boardgamegeek.db.model.CollectionViewFilterEntity
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.model.CollectionViewFilter
import com.boardgamegeek.export.model.CollectionViewForExport
import com.boardgamegeek.export.model.CollectionViewFilterForExport
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sorter.CollectionSorterFactory

fun CollectionViewEntity.mapToModel(filters: List<CollectionViewFilterEntity>? = null) = CollectionView(
    id = id,
    name = name.orEmpty(),
    sortType = sortType ?: CollectionSorterFactory.TYPE_UNKNOWN,
    count = selectedCount ?: 0,
    timestamp = selectedTimestamp ?: 0L,
    starred = starred == true,
    filters = filters?.let{ it.map { filter -> filter.mapToModel() } }
)

private fun CollectionViewFilterEntity.mapToModel() = CollectionViewFilter(
    id = id,
    type = type ?: CollectionFiltererFactory.TYPE_UNKNOWN,
    data = data.orEmpty(),
)

fun CollectionView.mapForExport() = CollectionViewForExport(
    name = this.name,
    sortType = this.sortType,
    starred = this.starred,
    filters = this.filters?.map { CollectionViewFilterForExport(it.type, it.data) }.orEmpty(),
)

fun CollectionViewForExport.mapToModel() = CollectionView(
    id = BggContract.INVALID_ID,
    name = name,
    sortType = sortType,
    count = 0,
    timestamp = 0L,
    starred = starred,
    filters = filters.map { it.mapToModel() },
)

private fun CollectionViewFilterForExport.mapToModel() = CollectionViewFilter(
    id = BggContract.INVALID_ID,
    type = this.type,
    data = this.data
)

fun CollectionView.mapToEntity() = CollectionViewEntity(
    id = if (id == BggContract.INVALID_ID) 0 else id,
    name = name,
    sortType = sortType,
    selectedCount = count,
    selectedTimestamp = timestamp,
    starred = starred,
)

fun CollectionViewFilter.mapToEntity(viewId: Int) = CollectionViewFilterEntity(
    id = if (id == BggContract.INVALID_ID) 0 else id,
    viewId = viewId,
    type = type,
    data = data,
)
