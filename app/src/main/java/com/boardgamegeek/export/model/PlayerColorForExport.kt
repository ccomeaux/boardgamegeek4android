package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

data class PlayerColorForExport(
    @Expose val sort: Int,
    @Expose val color: String
)
