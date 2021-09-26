package com.boardgamegeek.livedata

import androidx.paging.PositionalDataSource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.Game
import com.boardgamegeek.provider.BggContract
import timber.log.Timber
import java.io.IOException

class CommentsDataSource(private val gameId: Int, private val byRating: Boolean) : PositionalDataSource<Game.Comment>() {
    private val bggService = Adapter.createForXml()
    private var totalCount: Int = 0
    private val pageSize = Game.PAGE_SIZE
    private var priorPage = 0

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Game.Comment>) {
        try {
            if (gameId == BggContract.INVALID_ID) return

            val call = if (byRating) bggService.thingWithRatings(gameId, 1) else bggService.thingWithComments(gameId, 1)
            val response = call.execute()
            if (response.isSuccessful) {
                priorPage = 1
                val comments = response.body()?.games?.getOrNull(0)?.comments
                totalCount = comments?.totalitems ?: 0
                callback.onResult(comments?.comments.orEmpty(), 0, totalCount)
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Game.Comment>) {
        if (gameId == BggContract.INVALID_ID) return
        if (params.loadSize <= 0) return
        if (params.startPosition >= totalCount) return
        val page = (params.startPosition + pageSize) / pageSize
        if (page <= priorPage) return

        try {
            val call = if (byRating) bggService.thingWithRatings(gameId, page) else bggService.thingWithComments(gameId, page)
            val response = call.execute()
            if (response.isSuccessful) {
                priorPage = page
                val comments = response.body()?.games?.getOrNull(0)?.comments
                callback.onResult(comments?.comments.orEmpty())
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }
}