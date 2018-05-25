package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game

class GameLiveData(context: Context, private val gameId: Int) : ContentObservableLiveData<Game>(context) {
    override var uri = BggContract.Games.buildGameUri(gameId)

    override fun loadData() {
        val cursor = context.contentResolver.query(
                uri,
                Game.projection,
                null,
                null,
                null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                postValue(Game.fromCursor(c))
            } else {
                postValue(null)
            }
        }
    }
}
