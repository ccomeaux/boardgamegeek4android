package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.GeekListResponse
import com.boardgamegeek.livedata.NetworkLoader
import com.boardgamegeek.mappers.GeekListItemMapper
import com.boardgamegeek.ui.model.GeekList
import retrofit2.Call

class GeekListRepository(val application: BggApplication) {
    fun getGeekList(geekListId: Int): LiveData<RefreshableResource<GeekList>> {
        return object : NetworkLoader<GeekList, GeekListResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_geeklist

            override fun createCall(): Call<GeekListResponse> {
                return Adapter.createForXml().geekList(geekListId, 1)
            }

            override fun parseResult(result: GeekListResponse): GeekList {
                return GeekListItemMapper().map(result)
            }
        }.asLiveData()
    }
}
