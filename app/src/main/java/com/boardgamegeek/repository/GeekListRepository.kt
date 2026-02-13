package com.boardgamegeek.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.GeekList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

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

    class GeekListsPagingSource(private val sort: BggAjaxApi.GeekListSort, private val ajaxApi: BggAjaxApi) :
        PagingSource<Int, GeekList>() {
        override fun getRefreshKey(state: PagingState<Int, GeekList>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GeekList> {
            return try {
                val page = params.key ?: 1
                val response = withContext(Dispatchers.IO) {
                    ajaxApi.geekLists(sort, GeekListsResponse.PAGE_SIZE, page)
                }
                val geekLists = response.lists.map { it.mapToModel() }
                LoadResult.Page(geekLists, null, getNextPage(geekLists, page))
            } catch (e: Exception) {
                if (e is HttpException) {
                    Timber.w("Error code: ${e.code()}\n${e.response()?.body()}")
                } else {
                    Timber.w(e)
                }
                LoadResult.Error(e)
            }
        }

        private fun getNextPage(geekLists: List<GeekList>, currentPage: Int): Int? {
            return if (
                geekLists.isNotEmpty() &&
                currentPage * GeekListsResponse.PAGE_SIZE < GeekListsResponse.TOTAL_COUNT
            ) currentPage + 1
            else null
        }
    }
}
