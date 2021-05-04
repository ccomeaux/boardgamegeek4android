package com.boardgamegeek.mappers

import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.io.model.SearchResponse
import com.boardgamegeek.io.model.SearchResult

fun SearchResult.mapToEntity() = SearchResultEntity(
        id = id,
        name = name,
        yearPublished = yearPublished.asYear(),
        nameType
)

fun SearchResponse.mapToEntity() = this.items?.map { it.mapToEntity() }.orEmpty()
