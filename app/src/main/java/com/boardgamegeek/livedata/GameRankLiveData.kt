package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.database.ContentObserver
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.GameRank

class GameRankLiveData(val context: Context, val gameId: Int) : MutableLiveData<List<GameRank>>() {
    private val contentObserver = Observer()
    private val uri = BggContract.Games.buildRanksUri(gameId)
    private val dao = GameDao(context)

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
        postValue(dao.loadRanks(gameId))
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }
}
