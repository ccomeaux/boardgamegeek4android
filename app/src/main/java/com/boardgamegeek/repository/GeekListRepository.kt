package com.boardgamegeek.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.livedata.GeekListsPagingSource
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.GeekList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeekListRepository(
    private val api: BggService,
    private val ajaxApi: BggAjaxApi,
) {
   suspend fun getGeekList(geekListId: Int) = withContext(Dispatchers.IO) {
        val response = api.geekList(geekListId, 1)
        response.mapToModel()
    }

    fun loadPager(sort: @JvmSuppressWildcards BggAjaxApi.GeekListSort): Pager<Int, GeekList> =
        Pager(PagingConfig(GeekListsResponse.PAGE_SIZE)) {
            GeekListsPagingSource(sort, ajaxApi)
        }
}
