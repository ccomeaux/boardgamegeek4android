package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

data class CollectionViewFilterForExport(
    @Expose val type: Int,
    @Expose val data: String
)
