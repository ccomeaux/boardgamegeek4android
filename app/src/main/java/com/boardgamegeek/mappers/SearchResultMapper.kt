package com.boardgamegeek.mappers

import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.io.model.SearchResultRemote

fun SearchResultRemote.mapToModel() = SearchResult(
    id = id,
    name = name,
    yearPublished = yearPublished.asYear(),
    nameType
)
