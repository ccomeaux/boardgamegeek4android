package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.GameCollectionItem

class GamePollLiveData(context: Context, private val gameId: Int) :
        ContentObservableLiveData<GamePollEntity>(context) {
    private var dao = GameDao(context)

    override var uri = GameCollectionItem.uri

    override fun loadData() {
        postValue(GamePollEntity(dao.loadPoll(gameId, BggContract.POLL_TYPE_LANGUAGE_DEPENDENCE)))
    }
}
