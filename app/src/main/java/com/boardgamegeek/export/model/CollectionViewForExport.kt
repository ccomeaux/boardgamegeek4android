package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

data class CollectionViewForExport(
    @Expose val name: String,
    @Expose val sortType: Int,
    @Expose val starred: Boolean,
    @Expose val filters: List<CollectionViewFilterForExport>
) : Model()