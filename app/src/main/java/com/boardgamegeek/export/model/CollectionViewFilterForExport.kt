package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

class CollectionViewFilterForExport(@Expose val type: Int, @Expose val data: String)