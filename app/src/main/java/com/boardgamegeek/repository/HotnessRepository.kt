package com.boardgamegeek.repository

import com.boardgamegeek.entities.HotGame
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HotnessRepository(private val api: BggService) {
    suspend fun getHotness(): List<HotGame> = withContext(Dispatchers.IO) {
        val response = api.getHotness(BggService.HotnessType.BOARDGAME)
        response.games.map { it.mapToModel() }
    }
}
