package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HotnessRepository(val application: BggApplication) {
    suspend fun getHotness(): List<HotGameEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().getHotness(BggService.HOTNESS_TYPE_BOARDGAME)
        response.mapToEntity()
    }
}
