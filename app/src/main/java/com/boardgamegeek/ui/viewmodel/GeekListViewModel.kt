package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GeekListRepository
import com.boardgamegeek.entities.GeekListEntity

class GeekListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeekListRepository(getApplication())

    private val _geekListId = MutableLiveData<Int>()

    fun setId(geekListId: Int?) {
        if (_geekListId.value != geekListId) _geekListId.value = geekListId
    }

    val geekList: LiveData<RefreshableResource<GeekListEntity>> = Transformations.switchMap(_geekListId) { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> repository.getGeekList(id)
        }
    }
}