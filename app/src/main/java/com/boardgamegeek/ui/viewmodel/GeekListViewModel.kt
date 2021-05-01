package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GeekListRepository

class GeekListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeekListRepository()

    private val _geekListId = MutableLiveData<Int>()

    fun setId(geekListId: Int) {
        if (_geekListId.value != geekListId) _geekListId.value = geekListId
    }

    val geekList: LiveData<RefreshableResource<GeekListEntity>> = _geekListId.switchMap { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                liveData {
                    try {
                        emit(RefreshableResource.refreshing(null))
                        val geekList = repository.getGeekList(id)
                        emit(RefreshableResource.success(geekList))
                    } catch (e: Exception) {
                        emit(RefreshableResource.error<GeekListEntity>(e, application))
                    }
                }
            }
        }
    }
}
