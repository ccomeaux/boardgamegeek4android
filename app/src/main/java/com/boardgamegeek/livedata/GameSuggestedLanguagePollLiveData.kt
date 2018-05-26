package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.GameSuggestedLanguagePollEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.GameCollectionItem

class GameSuggestedLanguagePollLiveData(context: Context, private val gameId: Int) :
        ContentObservableLiveData<GameSuggestedLanguagePollEntity>(context) {
    private var dao = GameDao(context)

    override var uri = GameCollectionItem.uri

    override fun loadData() {
        postValue(GameSuggestedLanguagePollEntity(dao.loadPoll(gameId, BggContract.POLL_TYPE_LANGUAGE_DEPENDENCE)))
    }
}
