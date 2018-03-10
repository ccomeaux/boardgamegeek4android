package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game

class GameLiveData(val context: Context, gameId: Int) : MutableLiveData<Game>() {
    private val contentObserver = Observer()
    private var uri = Uri.EMPTY

    init {
        uri = BggContract.Games.buildGameUri(gameId)
        registerContentObserver()
        loadData()
    }

    override fun onActive() {
        super.onActive()
        context.contentResolver.registerContentObserver(uri, false, contentObserver)
    }

    override fun onInactive() {
        super.onInactive()
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    private fun registerContentObserver() {
        context.contentResolver.registerContentObserver(uri, false, contentObserver)
    }

    private fun loadData() {
        val cursor = context.contentResolver.query(
                uri,
                Game.projection,
                null,
                null,
                null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                postValue(Game.fromCursor(c))
            }
        }
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }
}
