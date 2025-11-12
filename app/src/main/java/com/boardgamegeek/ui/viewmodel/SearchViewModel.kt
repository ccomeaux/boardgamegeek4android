package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.repository.SearchRepository

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _query = MutableLiveData<Pair<String, Boolean>>()
    val query: LiveData<Pair<String, Boolean>>
        get() = _query

    private val repository = SearchRepository(getApplication())

    fun search(query: String) {
        if (_query.value?.first != query) _query.value = (query to true)
    }

    fun searchInexact(query: String) {
        if (_query.value?.first != query || _query.value?.second != false) _query.value = query to false
    }

    val searchResults: LiveData<RefreshableResource<List<SearchResultEntity>>> = _query.switchMap() { q ->
        when {
            q.first.isNotBlank() -> repository.search(q.first, q.second)
            else -> AbsentLiveData.create()
        }
    }
}