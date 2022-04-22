package com.boardgamegeek.mappers

import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.export.model.CollectionView
import com.boardgamegeek.export.model.Filter

fun CollectionViewEntity.mapToExportable() = CollectionView(
    name = this.name,
    sortType = this.sortType,
    starred = false,
    filters = this.filters?.map { Filter(it.type, it.data) }.orEmpty(),
)
