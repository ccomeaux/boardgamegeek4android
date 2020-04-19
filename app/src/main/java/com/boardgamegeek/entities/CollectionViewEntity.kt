package com.boardgamegeek.entities

import com.boardgamegeek.sorter.CollectionSorterFactory

data class CollectionViewEntity(
        val id: Long,
        val name: String,
        val sortType: Int = CollectionSorterFactory.TYPE_UNKNOWN,
        val filters: List<CollectionViewFilterEntity>? = null
) {
    override fun toString(): String = name
}
