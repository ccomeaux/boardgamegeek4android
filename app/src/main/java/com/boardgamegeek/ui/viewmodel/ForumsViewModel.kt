package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForumsViewModel @Inject constructor(
    application: Application,
    private val repository: ForumRepository,
) : AndroidViewModel(application) {
    private enum class ForumType {
        GAME,
        REGION,
        PERSON,
        COMPANY,
    }

    private val _id = MutableLiveData<Pair<ForumType, Int>>()

    fun setRegion() {
        if (_id.value?.first != ForumType.REGION) _id.value = (ForumType.REGION to BggService.ForumRegion.BOARDGAME.id)
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

    val forums: LiveData<RefreshableResource<Map<String, List<Forum>>>> = _id.switchMap { pair ->
        liveData {
            emit(RefreshableResource.refreshing(latestValue?.data))
            emit(
                try {
                    if (pair.second == BggContract.INVALID_ID) {
                        RefreshableResource.error("Invalid ID!")
                    } else {
                        val list = when (pair.first) {
                            ForumType.REGION -> repository.loadForRegion()
                            ForumType.COMPANY -> repository.loadForCompany(pair.second)
                            ForumType.GAME -> repository.loadForGame(pair.second)
                            ForumType.PERSON -> repository.loadForPerson(pair.second)
                        }
                        val map = mutableMapOf<String, List<Forum>>()
                        var currentHeader = ""
                        list.forEach { forum ->
                            if (forum.isHeader) {
                                currentHeader = forum.title
                            } else {
                                map[currentHeader] = map[currentHeader].orEmpty() + forum
                            }
                        }
                        RefreshableResource.success(map)
                    }
                } catch (e: Exception) {
                    RefreshableResource.error(e, application)
                }
            )
        }
    }
//
//    val forumsGroupedByHeader: LiveData<Map<String, List<Forum>>> = forums.map {
//        val map = mutableMapOf<String, List<Forum>>()
//        var currentHeader: String? = null
//        it.data?.forEach { f ->
//            if (f.isHeader) {
//                currentHeader = f.title
//            } else {
//                map[currentHeader.orEmpty()] = map[currentHeader.orEmpty()].orEmpty() + f
//            }
//        }
//        map
//    }
}
