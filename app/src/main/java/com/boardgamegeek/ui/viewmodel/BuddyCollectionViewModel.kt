package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.UserRepository

class BuddyCollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(getApplication())

    private val _id = MutableLiveData<Pair<String, String>>()

    fun setUsername(username: String) {
        if (_id.value?.first != username)
            _id.value = username to (_id.value?.second ?: DEFAULT_STATUS)
    }

    fun setStatus(status: String) {
        if (_id.value?.second != status) _id.value = (_id.value?.first ?: "") to status
    }

    val status: LiveData<String> = Transformations.map(_id) {
        it.second
    }

    val collection: LiveData<RefreshableResource<List<CollectionItemEntity>>> = Transformations.switchMap(_id) {
        when (it) {
            null -> AbsentLiveData.create()
            else -> userRepository.loadCollection(it.first, it.second)
        }
    }

    companion object {
        const val DEFAULT_STATUS = "own"
    }
}