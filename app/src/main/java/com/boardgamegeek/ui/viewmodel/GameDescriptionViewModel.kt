package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.database.ContentObserver
import com.boardgamegeek.BggApplication
import com.boardgamegeek.provider.BggContract

class GameDescriptionViewModel(application: Application) : AndroidViewModel(application) {
    private var description = MutableLiveData<String>()
    private val contentObserver = Observer()
    private var gameId = BggContract.INVALID_ID

    fun getGameDescription(gameId: Int): LiveData<String> {
        loadGame(gameId)
        return description
    }

    private fun loadGame(gameId: Int) {
        this.gameId = gameId
        loadData()
    }

    private fun loadData() {
        val uri = BggContract.Games.buildGameUri(gameId)
        getApplication<BggApplication>().contentResolver.registerContentObserver(uri, false, contentObserver)
        val cursor = getApplication<BggApplication>().contentResolver.query(
                uri,
                arrayOf(BggContract.Games.DESCRIPTION),
                null,
                null,
                null)
        cursor?.use { c ->
            if (c.moveToFirst())
                description.value = c.getString(0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<BggApplication>().contentResolver.unregisterContentObserver(contentObserver)
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) = loadData()
    }
}
