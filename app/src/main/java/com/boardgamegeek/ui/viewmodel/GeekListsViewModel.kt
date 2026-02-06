@file:OptIn(ExperimentalCoroutinesApi::class)

package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.repository.GeekListRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class GeekListsViewModel @Inject constructor(
    application: Application,
    private val repository: GeekListRepository,
) : AndroidViewModel(application) {
    private val _sort = MutableStateFlow(GeekList.SortType.entries.first())
    val sort: StateFlow<GeekList.SortType> = _sort

    fun setSort(sort: GeekList.SortType) {
        if (_sort.value != sort) {
            _sort.value = sort
            FirebaseAnalytics.getInstance(getApplication()).logEvent("Sort") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekLists")
                param("SortBy", sort.toString())
            }
        }
    }

    val geekLists = _sort.flatMapLatest {
        val sortString = when (it) {
            GeekList.SortType.Hot -> BggAjaxApi.GeekListSort.HOT
            GeekList.SortType.Recent -> BggAjaxApi.GeekListSort.RECENT
            GeekList.SortType.Active -> BggAjaxApi.GeekListSort.ACTIVE
        }
        repository.loadPager(sortString).flow.cachedIn(viewModelScope)
    }.cachedIn(viewModelScope)
}
