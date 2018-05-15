package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import com.boardgamegeek.ui.model.GameCollectionItem

class GameCollectionLiveData(val context: Context, private val gameId: Int) : MutableLiveData<List<GameCollectionItem>>() {
    private val contentObserver = Observer()
    private var uri = Uri.EMPTY

    init {
        uri = GameCollectionItem.uri
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
                GameCollectionItem.projection,
                GameCollectionItem.selection,
                GameCollectionItem.getSelectionArgs(gameId),
                null)
        cursor?.use {
            if (it.moveToFirst()) {
                val list = arrayListOf<GameCollectionItem>()
                do {
                    list.add(GameCollectionItem.fromCursor(context, it))
                } while (it.moveToNext())
                postValue(list)
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
