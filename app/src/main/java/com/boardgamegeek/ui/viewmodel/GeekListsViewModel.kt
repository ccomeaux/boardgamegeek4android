package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GeekListEntry
import com.boardgamegeek.io.model.GeekListsResponse
import com.boardgamegeek.livedata.GeekListsDataSource
import com.boardgamegeek.provider.BggContract

class GeekListsViewModel(application: Application) : AndroidViewModel(application) {
    private val _sort = MutableLiveData<String>()

    fun setSort(sort: SortType) {
        val sortString = when (sort) {
            SortType.HOT -> BggService.GEEK_LIST_SORT_HOT
            SortType.RECENT -> BggService.GEEK_LIST_SORT_RECENT
            SortType.ACTIVE -> BggService.GEEK_LIST_SORT_ACTIVE
        }
        if (_sort.value != sortString) _sort.value = sortString
    }

    private var dataSourceFactory: DataSource.Factory<Int, GeekListEntry> = GeekListsDataSourceFactory(BggService.GEEK_LIST_SORT_HOT)

    private val config = PagedList.Config.Builder()
            .setPageSize(GeekListsResponse.PAGE_SIZE)
            .setInitialLoadSizeHint(GeekListsResponse.PAGE_SIZE)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .build()

    val geekLists: LiveData<PagedList<GeekListEntity>> = Transformations.switchMap(_sort) {
        dataSourceFactory = GeekListsDataSourceFactory(it)
        LivePagedListBuilder(dataSourceFactory.map { item ->
            val id = if (item.href.isEmpty()) {
                BggContract.INVALID_ID
            } else {
                val start: Int = item.href.indexOf("/geeklist/") + 10
                val end = item.href.lastIndexOf("/")
                item.href.substring(start, end).toIntOrNull() ?: BggContract.INVALID_ID
            }

            GeekListEntity(
                    id,
                    item.title.trim(),
                    item.username.trim(),
                    numberOfItems = item.numitems,
                    numberOfThumbs = item.numpositive
            )
        }, config).build()
    }

    class GeekListsDataSourceFactory(private val sort: String) : DataSource.Factory<Int, GeekListEntry>() {
        override fun create(): DataSource<Int, GeekListEntry> {
            return GeekListsDataSource(sort)
        }
    }

    enum class SortType {
        HOT, RECENT, ACTIVE
    }
}
