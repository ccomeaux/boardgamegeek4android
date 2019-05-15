package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.PersonGameEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.MechanicRepository

class MechanicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MechanicRepository(getApplication())

    private val _mechanicId = MutableLiveData<Int>()

    fun setId(id: Int?) {
        if (_mechanicId.value != id) _mechanicId.value = id
    }

    val collection: LiveData<List<PersonGameEntity>> = Transformations.switchMap(_mechanicId) { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> repository.loadCollection(id)
        }
    }

    fun refresh() {
        _mechanicId.value?.let { _mechanicId.value = it }
    }
}