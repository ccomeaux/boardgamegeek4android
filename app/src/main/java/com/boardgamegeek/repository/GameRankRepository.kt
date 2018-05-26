package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.livedata.GameRankLiveData

class GameRankRepository(val application: BggApplication) {
    private val result = MediatorLiveData<List<GameRankEntity>>()

    fun getRanks(gameId: Int): LiveData<List<GameRankEntity>> {
        application.appExecutors.diskIO.execute {
            val dbSource = GameRankLiveData(application, gameId).load()
            result.addSource(dbSource) {
                result.postValue(it)
            }
        }
        return result
    }
}
