package com.boardgamegeek.repository

import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.mappers.mapToEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeekListRepository(
    private val api: BggService,
    private val ajaxApi: BggAjaxApi,
) {
    suspend fun getGeekLists(sort: BggAjaxApi.GeekListSort?, page: Int) = withContext(Dispatchers.IO) {
        val response = ajaxApi.geekLists(sort, GeekListsResponse.PAGE_SIZE, page)
        response.mapToEntity()
    }

    suspend fun getGeekList(geekListId: Int) = withContext(Dispatchers.IO) {
        val response = api.geekList(geekListId, 1)
        response.mapToEntity()
    }
}
