package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.entities.GameCommentEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import retrofit2.HttpException
import timber.log.Timber

class CommentsPagingSource(val gameId: Int, private val sortByRating: Boolean = false, val repository: GameRepository) :
    PagingSource<Int, GameCommentEntity>() {
    override fun getRefreshKey(state: PagingState<Int, GameCommentEntity>): Int? {
        return null // not sure this is correct, but I hope this will have paging start over from 1
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GameCommentEntity> {
        return try {
            if (gameId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid ID"))

            val page = params.key ?: 1
            val entity = if (sortByRating) repository.loadRatings(gameId, page) else repository.loadComments(gameId, page)
            val nextPage = getNextPage(page, params.loadSize, entity?.numberOfRatings ?: 0)
            LoadResult.Page(entity?.ratings.orEmpty(), null, nextPage)
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
