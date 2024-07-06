package com.boardgamegeek.livedata

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.model.GameComment
import com.boardgamegeek.io.model.GameRemote
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import retrofit2.HttpException
import timber.log.Timber

class CommentsPagingSource(val gameId: Int, private val sortByRating: Boolean = false, val repository: GameRepository) :
    PagingSource<Int, GameComment>() {
    override fun getRefreshKey(state: PagingState<Int, GameComment>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GameComment> {
        return try {
            if (gameId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid ID"))

            val page = params.key ?: 1
            val list = if (sortByRating) repository.loadRatings(gameId, page) else repository.loadComments(gameId, page)
            val nextPage = if (page * GameRemote.PAGE_SIZE < (list?.numberOfRatings ?: 0)) page + 1 else null
            LoadResult.Page(list?.ratings.orEmpty(), null, nextPage)
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
