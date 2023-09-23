package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.CollectionViewFilterLocal
import com.boardgamegeek.db.model.CollectionViewLocal
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.export.model.CollectionViewForExport
import com.boardgamegeek.export.model.CollectionViewFilterForExport
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sorter.CollectionSorterFactory

fun CollectionViewEntity.mapForExport() = CollectionViewForExport(
    name = this.name,
    sortType = this.sortType,
    starred = this.starred,
    filters = this.filters?.map { CollectionViewFilterForExport(it.type, it.data) }.orEmpty(),
)

fun CollectionViewForExport.mapToEntity() = CollectionViewEntity(
    id = BggContract.INVALID_ID,
    name = name,
    sortType = sortType,
    count = 0,
    timestamp = 0L,
    starred = starred,
    filters = filters.map { it.mapToEntity() },
)

fun CollectionViewFilterForExport.mapToEntity() = CollectionViewFilterEntity(
    id = BggContract.INVALID_ID,
    type = this.type,
    data = this.data
)

fun CollectionViewLocal.mapToEntity() = CollectionViewEntity(
    id = id,
    name = name.orEmpty(),
    sortType = sortType ?: CollectionSorterFactory.TYPE_UNKNOWN,
    count = selectedCount ?: 0,
    timestamp = selectedTimestamp ?: 0L,
    starred = starred == true,
    filters = filters?.map { it.mapToEntity() },
)

fun CollectionViewEntity.mapToLocal() = CollectionViewLocal(
    id = id,
    name = name,
    sortType = sortType,
    selectedCount = count,
    selectedTimestamp = timestamp,
    starred = starred,
    filters = filters?.map { it.mapToLocal(this.id) },
)

fun CollectionViewFilterLocal.mapToEntity() = CollectionViewFilterEntity(
    id = id,
    type = type ?: CollectionFiltererFactory.TYPE_UNKNOWN,
    data = data.orEmpty(),
)

fun CollectionViewFilterEntity.mapToLocal(viewId: Int) = CollectionViewFilterLocal(
    id = id,
    viewId = viewId,
    type = type,
    data = data,
)
