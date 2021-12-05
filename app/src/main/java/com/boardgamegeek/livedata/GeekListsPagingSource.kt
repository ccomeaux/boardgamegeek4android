package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.repository.GeekListRepository
import retrofit2.HttpException
import timber.log.Timber

class GeekListsPagingSource(private val sort: String, private val repository: GeekListRepository) : PagingSource<Int, GeekListEntity>() {
    override fun getRefreshKey(state: PagingState<Int, GeekListEntity>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GeekListEntity> {
        return try {
            val page = params.key ?: 1
            val response = repository.getGeekLists(sort, page)
            val nextPage = if (response.isEmpty()) null else getNextPage(page, GeekListsResponse.PAGE_SIZE, GeekListsResponse.TOTAL_COUNT)
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

    private fun getNextPage(currentPage: Int, pageSize: Int, totalCount: Int): Int? {
        return if (currentPage * pageSize < totalCount) currentPage + 1 else null
    }
}
