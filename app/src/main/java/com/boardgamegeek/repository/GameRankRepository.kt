package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.GameRank
import com.boardgamegeek.livedata.GameRankLiveData

class GameRankRepository(val application: BggApplication) {
    private val result = MediatorLiveData<List<GameRank>>()

    fun getRanks(gameId: Int): LiveData<List<GameRank>> {
        application.appExecutors.diskIO.execute {
            val dbSource = GameRankLiveData(application, gameId)
            result.addSource(dbSource) {
                result.postValue(it)
            }
        }
        return result
    }
}
