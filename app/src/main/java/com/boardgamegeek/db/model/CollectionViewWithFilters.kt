package com.boardgamegeek.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class CollectionViewWithFilters(
    @Embedded
    val view: CollectionViewEntity,
    @Relation(parentColumn = "_id", entityColumn = "filter_id")
    val filters: List<CollectionViewFilterEntity>,
)
