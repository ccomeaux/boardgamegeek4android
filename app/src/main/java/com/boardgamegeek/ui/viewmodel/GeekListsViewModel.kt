package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.boardgamegeek.io.BggAjaxApi
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.livedata.GeekListsPagingSource
import com.boardgamegeek.repository.GeekListRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GeekListsViewModel @Inject constructor(
    application: Application,
    private val repository: GeekListRepository,
) : AndroidViewModel(application) {
    private val _sort = MutableLiveData<BggAjaxApi.GeekListSort>()

    fun setSort(sort: SortType) {
        val sortString = when (sort) {
            SortType.HOT -> BggAjaxApi.GeekListSort.HOT
            SortType.RECENT -> BggAjaxApi.GeekListSort.RECENT
            SortType.ACTIVE -> BggAjaxApi.GeekListSort.ACTIVE
        }
        if (_sort.value != sortString) {
            _sort.value = sortString
            FirebaseAnalytics.getInstance(getApplication()).logEvent("Sort") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekLists")
                param("SortBy", sort.toString())
            }
        }
    }

    val geekLists = _sort.switchMap { sort ->
        Pager(PagingConfig(GeekListsResponse.PAGE_SIZE)) {
            GeekListsPagingSource(sort, repository)
        }.liveData.cachedIn(this)
    }

    enum class SortType {
        HOT, RECENT, ACTIVE
    }
}
