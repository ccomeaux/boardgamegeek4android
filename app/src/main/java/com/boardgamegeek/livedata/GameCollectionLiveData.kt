package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.ui.model.GameCollectionItem

class GameCollectionLiveData(context: Context, private val gameId: Int) : ContentObservableLiveData<List<GameCollectionItem>>(context) {
    override var uri = GameCollectionItem.uri

    override fun loadData() {
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
}
