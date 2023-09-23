package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

data class GameForExport(
    @Expose val id: Int,
    @Expose val colors: List<ColorForExport>
) : ExportModel()
