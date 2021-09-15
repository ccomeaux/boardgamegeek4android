package com.boardgamegeek.export.model

import com.google.gson.annotations.Expose

class CollectionView(@Expose val name: String, @Expose val sortType: Int, @Expose val starred: Boolean, @Expose val filters: List<Filter>) : Model()