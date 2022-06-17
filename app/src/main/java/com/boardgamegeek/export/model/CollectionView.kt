package com.boardgamegeek.export.model

import com.boardgamegeek.provider.BggContract
import com.google.gson.annotations.Expose

data class CollectionView(
    @Expose val name: String,
    @Expose val sortType: Int,
    @Expose val starred: Boolean,
    @Expose val filters: List<Filter>
) : Model() {
    val id: Long = BggContract.INVALID_ID.toLong()
}
