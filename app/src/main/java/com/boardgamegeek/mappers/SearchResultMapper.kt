package com.boardgamegeek.mappers

import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.io.model.SearchResultRemote

fun SearchResultRemote.mapToModel() = SearchResult(
    id = id,
    name = name,
    yearPublished = yearPublished.asYear(),
    when (nameType) {
        SearchResultRemote.NAME_TYPE_PRIMARY -> SearchResult.NameType.Primary
        SearchResultRemote.NAME_TYPE_ALTERNATE -> SearchResult.NameType.Alternate
        else -> SearchResult.NameType.Unknown
    }
)
