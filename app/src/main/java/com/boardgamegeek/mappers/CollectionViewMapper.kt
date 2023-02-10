package com.boardgamegeek.mappers

import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.export.model.CollectionView
import com.boardgamegeek.export.model.Filter

fun CollectionViewEntity.mapToExportable() = CollectionView(
    name = this.name,
    sortType = this.sortType,
    starred = this.starred,
    filters = this.filters?.map { Filter(it.type, it.data) }.orEmpty(),
)

fun CollectionView.mapToEntity() = CollectionViewEntity(
    id = this.id,
    name = this.name,
    sortType = this.sortType,
    starred = this.starred,
    filters = this.filters.map { it.mapToEntity() },
)

fun Filter.mapToEntity() = CollectionViewFilterEntity(
    type = this.type,
    data = this.data
)
