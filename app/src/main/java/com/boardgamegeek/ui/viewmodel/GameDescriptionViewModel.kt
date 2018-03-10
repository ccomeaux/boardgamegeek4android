package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository

class GameDescriptionViewModel(application: Application) : AndroidViewModel(application) {
    private var description: LiveData<String>? = null
    private var gameId = BggContract.INVALID_ID

    fun init(gameId: Int) {
        this.gameId = gameId
        if (description == null) description = GameRepository(getApplication()).getGameDescription(gameId)
    }

    fun getDescription(): LiveData<String> {
        return description ?: MutableLiveData()
    }
}
