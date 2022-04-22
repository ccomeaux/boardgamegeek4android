package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository

class ForumsViewModel(application: Application) : AndroidViewModel(application) {
    private enum class ForumType {
        GAME,
        REGION,
        PERSON,
        COMPANY,
    }

    private val _id = MutableLiveData<Pair<ForumType, Int>>()

    private val repository = ForumRepository(application)

    fun setRegion() {
        if (_id.value?.first != ForumType.REGION) _id.value = (ForumType.REGION to BggService.FORUM_REGION_BOARDGAME)
    }

    fun setGameId(gameId: Int) {
        if (_id.value != ForumType.GAME to gameId) _id.value = (ForumType.GAME to gameId)
    }

    fun setPersonId(personId: Int) {
        if (_id.value != ForumType.PERSON to personId) _id.value = (ForumType.PERSON to personId)
    }

    fun setCompanyId(companyId: Int) {
        if (_id.value != ForumType.COMPANY to companyId) _id.value = (ForumType.COMPANY to companyId)
    }

    val forums: LiveData<RefreshableResource<List<ForumEntity>>> = _id.switchMap { pair ->
        liveData {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emit(
                try {
                    if (pair.second == BggContract.INVALID_ID) {
                        RefreshableResource.error("Invalid ID!")
                    } else {
                        RefreshableResource.success(
                            when (pair.first) {
                                ForumType.REGION -> repository.loadForRegion()
                                ForumType.COMPANY -> repository.loadForCompany(pair.second)
                                ForumType.GAME -> repository.loadForGame(pair.second)
                                ForumType.PERSON -> repository.loadForPerson(pair.second)
                            }
                        )
                    }
                } catch (e: Exception) {
                    RefreshableResource.error(e, application)
                }
            )
        }
    }
}
