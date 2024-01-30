package com.boardgamegeek.model

import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sorter.CollectionSorterFactory

data class CollectionView(
    val id: Int = BggContract.INVALID_ID,
    val name: String,
    val sortType: Int = CollectionSorterFactory.TYPE_DEFAULT,
    val count: Int = 0,
    val timestamp: Long = 0L,
    val starred: Boolean = false,
    val filters: List<CollectionFilterer>? = null,
) {
    override fun toString(): String = name
}
