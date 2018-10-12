package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.SearchResponse
import com.boardgamegeek.livedata.NetworkLoader
import com.boardgamegeek.mappers.SearchResultMapper
import retrofit2.Call
import java.util.*

class SearchRepository(val application: BggApplication) {
    fun search(query: String, exact: Boolean): LiveData<RefreshableResource<List<SearchResultEntity>>> {
        return object : NetworkLoader<List<SearchResultEntity>, SearchResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_search

            override fun createCall(): Call<SearchResponse> {
                return Adapter.createForXml().search(query, BggService.SEARCH_TYPE_BOARD_GAME, if (exact) 1 else 0)
            }

            override fun parseResult(result: SearchResponse): List<SearchResultEntity> {
                val results = ArrayList<SearchResultEntity>()
                val mapper = SearchResultMapper()
                result.items?.forEach {
                    results.add(mapper.map(it))
                }
                return results
            }
        }.asLiveData()
    }
}