package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.repository.GeekListRepository
import retrofit2.HttpException
import timber.log.Timber

class GeekListsPagingSource(private val sort: BggService.GeekListSort, private val repository: GeekListRepository) :
    PagingSource<Int, GeekListEntity>() {
    override fun getRefreshKey(state: PagingState<Int, GeekListEntity>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GeekListEntity> {
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
