package com.boardgamegeek.repository

import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HotnessRepository(private val api: BggService) {
    suspend fun getHotness(): List<HotGameEntity> = withContext(Dispatchers.IO) {
        val response = api.getHotness(BggService.HotnessType.BOARDGAME)
        response.mapToEntity()
    }
}
