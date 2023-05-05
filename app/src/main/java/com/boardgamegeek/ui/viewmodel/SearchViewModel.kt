package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.SearchRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    private val repository: SearchRepository,
    private val playRepository: PlayRepository
) : AndroidViewModel(application) {
    private val _query = MutableLiveData<Pair<String, Boolean>>()
    val query: LiveData<Pair<String, Boolean>>
        get() = _query

    fun search(query: String) {
        FirebaseAnalytics.getInstance(getApplication()).logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
            param("exact", true.toString())
        }
        if (_query.value?.first != query) _query.value = (query to true)
    }

    fun searchInexact(query: String) {
        FirebaseAnalytics.getInstance(getApplication()).logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
            param("exact", false.toString())
        }
        if (_query.value?.first != query || _query.value?.second != false) _query.value = query to false
    }

    val searchResults: LiveData<RefreshableResource<List<SearchResultEntity>>> = _query.switchMap { q ->
        liveData {
            when {
                q.first.isNotBlank() ->
                    try {
                        emit(RefreshableResource.refreshing(null))
                        val results = repository.search(q.first, q.second)
                        if (results.isEmpty() && q.second) {
                            searchInexact(q.first)
                        } else {
                            emit(RefreshableResource.success(results))
                        }
                    } catch (e: Exception) {
                        emit(RefreshableResource.error(e, application))
                    }
                else -> emit(RefreshableResource.success(null))
            }
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            playRepository.logQuickPlay(gameId, gameName)
        }
    }
}
