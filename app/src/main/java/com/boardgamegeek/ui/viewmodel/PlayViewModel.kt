package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.PlayRepository

class PlayViewModel(application: Application) : AndroidViewModel(application) {
    val repository = PlayRepository(getApplication())

    private val internalId = MutableLiveData<Long>()

    val play: LiveData<PlayEntity> = Transformations.switchMap(internalId) { id ->
        when (id) {
            null -> AbsentLiveData.create()
            else -> repository.getPlay(id)
        }
    }

    fun setId(id: Long) {
        if (internalId.value != id) internalId.value = id
    }
}