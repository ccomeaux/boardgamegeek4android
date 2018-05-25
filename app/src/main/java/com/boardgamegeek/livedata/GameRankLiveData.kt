package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.database.ContentObserver
import com.boardgamegeek.ui.model.GameRank

class GameRankLiveData(val context: Context, gameId: Int) : MutableLiveData<List<GameRank>>() {
    private val contentObserver = Observer()
    private val uri = GameRank.buildUri(gameId)

    init {
        registerContentObserver()
        loadData()
    }

    override fun onActive() {
        super.onActive()
        registerContentObserver()
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
                GameRank.PROJECTION,
                null,
                null,
                null)
        cursor?.use {
            if (it.moveToFirst()) {
                val ranks = arrayListOf<GameRank>()
                do {
                    ranks.add(GameRank.fromCursor(it))
                } while (it.moveToNext())
                postValue(ranks)
            } else {
                postValue(null)
            }
        }
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }
}
