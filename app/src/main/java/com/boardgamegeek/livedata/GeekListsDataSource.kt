package com.boardgamegeek.livedata

import androidx.paging.PositionalDataSource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.GeekListEntry
import com.boardgamegeek.io.model.GeekListsResponse
import timber.log.Timber
import java.io.IOException

class GeekListsDataSource(private val sort: String) : PositionalDataSource<GeekListEntry>() {
    private val bggService = Adapter.createForJson()
    private val totalCount: Int = GeekListsResponse.TOTAL_COUNT
    private val pageSize = GeekListsResponse.PAGE_SIZE
    private var priorPage: Int = 0

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<GeekListEntry>) {
        try {
            val response = bggService.geekLists(sort, pageSize, 1).execute()
            if (response.isSuccessful) {
                priorPage = 1
                callback.onResult(response.body()?.lists.orEmpty(), 0)
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w("Error code: ${e.message}")
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<GeekListEntry>) {
        if (params.loadSize <= 0) return
        if (params.startPosition >= totalCount) return
        val page = (params.startPosition + pageSize) / pageSize
        if (page <= priorPage) return

        try {
            val response = bggService.geekLists(sort, pageSize, page).execute()
            if (response.isSuccessful) {
                priorPage = page
                callback.onResult(response.body()?.lists.orEmpty())
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w("Error code: ${e.message}")
        }
    }
}