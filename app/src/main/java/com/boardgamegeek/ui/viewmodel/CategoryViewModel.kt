package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CategoryRepository

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CategoryRepository(getApplication())

    private val _categoryId = MutableLiveData<Int>()

    fun setId(id: Int?) {
        if (_categoryId.value != id) _categoryId.value = id
    }

    val collection: LiveData<List<BriefGameEntity>> = Transformations.switchMap(_categoryId) { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> repository.loadCollection(id)
        }
    }

    fun refresh() {
        _categoryId.value?.let { _categoryId.value = it }
    }
}