package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.ThreadEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import retrofit2.HttpException
import timber.log.Timber

class ForumPagingSource(private val forumId: Int?, private val repository: ForumRepository) : PagingSource<Int, ThreadEntity>() {
    override fun getRefreshKey(state: PagingState<Int, ThreadEntity>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ThreadEntity> {
        return try {
            if (forumId == null) return LoadResult.Error(Exception("Null ID"))
            if (forumId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid Forum ID"))

            val currentPage = params.key ?: 1
            val forumEntity = repository.loadForum(forumId, currentPage)
            val nextPage = getNextPage(currentPage, params.loadSize, forumEntity.numberOfThreads)
            LoadResult.Page(forumEntity.threads, null, nextPage)
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
