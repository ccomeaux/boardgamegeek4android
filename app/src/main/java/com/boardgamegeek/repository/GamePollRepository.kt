package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.GameSuggestedLanguagePollEntity
import com.boardgamegeek.livedata.GameSuggestedLanguagePollLiveData

class GamePollRepository(val application: BggApplication) {
    private val language = MediatorLiveData<GameSuggestedLanguagePollEntity>()

    fun getLanguagePoll(gameId: Int): LiveData<GameSuggestedLanguagePollEntity> {
        application.appExecutors.diskIO.execute {
            val dbSource = GameSuggestedLanguagePollLiveData(application, gameId).load()
            language.addSource(dbSource) {
                language.postValue(it)
            }
        }
        return language
    }
}
