package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.ThreadEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import retrofit2.HttpException
import timber.log.Timber

class ForumDataSource(
        private val forumId: Int?,
        private val repository: ForumRepository
) : PagingSource<Int, ThreadEntity>() {
    override fun getRefreshKey(state: PagingState<Int, ThreadEntity>): Int? {
        return null // not sure this is correct, but I hope this will have paging start over from 1
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ThreadEntity> {
        return try {
            if (forumId == null) return LoadResult.Error(Exception("Null ID"))
            if (forumId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid Forum ID"))

            val page = params.key ?: 1

            val entity = repository.loadForum(forumId, page)

            val nextPage = if (page * params.loadSize < entity.numberOfThreads) {
                page + 1
            } else null

            LoadResult.Page(entity.threads, null, nextPage)
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
