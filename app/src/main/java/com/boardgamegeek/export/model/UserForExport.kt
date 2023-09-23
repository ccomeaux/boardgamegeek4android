package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

data class UserForExport(
    @Expose val name: String,
    @Expose val colors: List<PlayerColorForExport>
) : ExportModel()
