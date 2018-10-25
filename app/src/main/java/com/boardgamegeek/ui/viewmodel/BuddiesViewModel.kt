package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.repository.UserRepository
import java.util.concurrent.TimeUnit

class BuddiesViewModel(application: Application) : AndroidViewModel(application) {
    private val refreshTimestamp = MutableLiveData<Long>()
    private val userRepository = UserRepository(getApplication())

    init {
        refreshTimestamp.value = System.currentTimeMillis()
    }

    val buddies: LiveData<RefreshableResource<List<UserEntity>>> = Transformations.switchMap(refreshTimestamp) {
        userRepository.loadBuddies()
    }

    fun refresh(): Boolean {
        return if (refreshTimestamp.value?.isOlderThan(5, TimeUnit.DAYS) == true) {
            refreshTimestamp.value = System.currentTimeMillis()
            true
        } else {
            false
        }
    }
}