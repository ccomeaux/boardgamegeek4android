package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.SearchRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    private val repository: SearchRepository,
    private val playRepository: PlayRepository
) : AndroidViewModel(application) {
    data class Query(
        val text: String = "",
        val exact: Boolean = true,
    )

    data class SearchResults(
        val query: Query,
        val results: List<SearchResult>
    ) {
        companion object {
            val EMPTY = SearchResults(Query(), emptyList())
        }
    }

    private val _query = MutableLiveData<Query>()
    val query: LiveData<Query>
        get() = _query

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _loggedPlayResult = MutableLiveData<Event<PlayUploadResult>>()
    val loggedPlayResult: LiveData<Event<PlayUploadResult>>
        get() = _loggedPlayResult

    fun search(text: String) {
        FirebaseAnalytics.getInstance(getApplication()).logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, text)
            param("exact", true.toString())
        }
        if (_query.value?.text != text) _query.value = Query(text, true)
    }

    fun searchInexact(text: String) {
        FirebaseAnalytics.getInstance(getApplication()).logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, text)
            param("exact", false.toString())
        }
        if (_query.value?.text != text || _query.value?.exact != false) _query.value = Query(text, false)
    }

    val searchResults: LiveData<RefreshableResource<SearchResults>> = _query.switchMap { query ->
        liveData {
            when {
                query.text.isNotBlank() ->
                    try {
                        emit(RefreshableResource.refreshing(latestValue?.data))
                        val results = repository.search(query.text, query.exact)
                        if (results.isEmpty() && query.exact) {
                            searchInexact(query.text)
                        } else {
                            emit(RefreshableResource.success(SearchResults(query, results)))
                        }
                    } catch (e: Exception) {
                        emit(RefreshableResource.error(e, application))
                    }
                else -> {
                    emit(RefreshableResource.success(SearchResults(query, emptyList())))
                }
            }
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure)
                postError(result.exceptionOrNull())
            else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID)
                        _loggedPlayResult.value = Event(it)
                }
            }
        }
    }

    private fun postError(exception: Throwable?) {
        _errorMessage.value = Event(exception?.message.orEmpty())
    }
}
