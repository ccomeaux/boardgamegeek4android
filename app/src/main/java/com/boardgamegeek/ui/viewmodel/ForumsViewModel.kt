package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository

class ForumsViewModel(application: Application) : AndroidViewModel(application) {
    private enum class ForumType {
        GAME,
        REGION,
        PERSON,
        COMPANY
    }

    private val _id = MutableLiveData<Pair<ForumType, Int>>()

    private val repository = ForumRepository(getApplication())

    fun setRegion() {
        if (_id.value?.first != ForumType.REGION) _id.value = (ForumType.REGION to BggContract.INVALID_ID)
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

    val forums: LiveData<RefreshableResource<List<ForumEntity>>> = Transformations.switchMap(_id) { pair ->
        when {
            pair.first == ForumType.REGION -> repository.getForums()
            pair.first == ForumType.GAME && pair.second != BggContract.INVALID_ID -> repository.getForumsForGame(pair.second)
            pair.first == ForumType.PERSON && pair.second != BggContract.INVALID_ID -> repository.getForumsForPerson(pair.second)
            pair.first == ForumType.COMPANY && pair.second != BggContract.INVALID_ID -> repository.getForumsForCompany(pair.second)
            else -> AbsentLiveData.create()
        }
    }

}