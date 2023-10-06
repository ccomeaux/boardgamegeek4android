package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.Thread
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import retrofit2.HttpException
import timber.log.Timber

class ForumPagingSource(private val forumId: Int?, private val repository: ForumRepository) : PagingSource<Int, Thread>() {
    override fun getRefreshKey(state: PagingState<Int, Thread>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Thread> {
        return try {
            if (forumId == null) return LoadResult.Error(Exception("Null ID"))
            if (forumId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid Forum ID"))

            val currentPage = params.key ?: 1
            val forum = repository.loadForum(forumId, currentPage)
            val nextPage = if (currentPage * ForumResponse.PAGE_SIZE < forum.numberOfThreads) currentPage + 1 else null
            LoadResult.Page(forum.threads, null, nextPage)
        } catch (e: Exception) {
            if (e is HttpException) {
                Timber.w("Error code: ${e.code()}\n${e.response()?.body()}")
            } else {
                Timber.w(e)
            }
            LoadResult.Error(e)
        }
    }
}
