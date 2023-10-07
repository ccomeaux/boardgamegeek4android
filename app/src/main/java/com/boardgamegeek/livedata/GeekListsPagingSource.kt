package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.GeekList
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.repository.GeekListRepository
import retrofit2.HttpException
import timber.log.Timber

class GeekListsPagingSource(private val sort: BggAjaxApi.GeekListSort, private val repository: GeekListRepository) :
    PagingSource<Int, GeekList>() {
    override fun getRefreshKey(state: PagingState<Int, GeekList>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GeekList> {
        return try {
            val page = params.key ?: 1
            val response = repository.getGeekLists(sort, page)
            val nextPage = if (response.isEmpty()) null else getNextPage(page)
            LoadResult.Page(response, null, nextPage)
        } catch (e: Exception) {
            if (e is HttpException) {
                Timber.w("Error code: ${e.code()}\n${e.response()?.body()}")
            } else {
                Timber.w(e)
            }
            LoadResult.Error(e)
        }
    }

    private fun getNextPage(currentPage: Int): Int? {
        return if (currentPage * GeekListsResponse.PAGE_SIZE < GeekListsResponse.TOTAL_COUNT) currentPage + 1 else null
    }
}
