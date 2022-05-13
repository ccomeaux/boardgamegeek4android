package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(val application: BggApplication) {
    suspend fun search(query: String, exact: Boolean): List<SearchResultEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().search(query, BggService.SearchType.BOARDGAME, if (exact) 1 else 0)
        response.mapToEntity()
    }
}
