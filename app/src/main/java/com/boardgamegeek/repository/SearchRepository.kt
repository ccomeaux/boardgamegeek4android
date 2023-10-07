package com.boardgamegeek.repository

import com.boardgamegeek.entities.SearchResult
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(private val api: BggService) {
    suspend fun search(query: String, exact: Boolean): List<SearchResult> = withContext(Dispatchers.IO) {
        val response = api.search(query, BggService.SearchType.BOARDGAME, if (exact) 1 else 0)
        response.items?.map { it.mapToModel() }.orEmpty()
    }
}
