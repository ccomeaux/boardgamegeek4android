package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.HotnessResponse
import com.boardgamegeek.livedata.NetworkLoader
import com.boardgamegeek.mappers.HotGameMapper
import retrofit2.Call

class HotnessRepository(val application: BggApplication) {
    fun getHotness(): LiveData<RefreshableResource<List<HotGameEntity>>> {
        return object : NetworkLoader<List<HotGameEntity>, HotnessResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_hotness

            override fun createCall(): Call<HotnessResponse> {
                return Adapter.createForXml().getHotness(BggService.HOTNESS_TYPE_BOARDGAME)
            }

            override fun parseResult(result: HotnessResponse): List<HotGameEntity> {
                val results = mutableListOf<HotGameEntity>()
                val mapper = HotGameMapper()
                result.games?.forEach {
                    results.add(mapper.map(it))
                }
                return results
            }
        }.asLiveData()
    }
}
