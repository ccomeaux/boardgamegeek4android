package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.boardgamegeek.entities.GameCommentEntity
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.io.model.Game
import com.boardgamegeek.livedata.CommentsDataSource
import com.boardgamegeek.provider.BggContract

class GameCommentsViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        RATING, USER
    }

    private val _id = MutableLiveData<Pair<Int, SortType>>()

    val sort: LiveData<SortType> = Transformations.map(_id) {
        _id.value?.second
    }

    fun setGameId(id: Int) {
        if (_id.value?.first != id) _id.value = id to (_id.value?.second ?: SortType.RATING)
    }

    fun setSort(sort: SortType) {
        if (_id.value?.second != sort) _id.value = (_id.value?.first
                ?: BggContract.INVALID_ID) to sort
    }

    private var dataSourceFactory: DataSource.Factory<Int, Game.Comment> = CommentsDataSourceFactory(BggContract.INVALID_ID, true)

    private val config = PagedList.Config.Builder()
            .setPageSize(ForumResponse.PAGE_SIZE)
            .setInitialLoadSizeHint(ForumResponse.PAGE_SIZE)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(true)
            .build()

    val comments: LiveData<PagedList<GameCommentEntity>> = Transformations.switchMap(_id) { id ->
        dataSourceFactory = CommentsDataSourceFactory(id.first, id.second == SortType.RATING)
        LivePagedListBuilder(dataSourceFactory.map { comment ->
            GameCommentEntity(
                    comment.username,
                    comment.rating.toDoubleOrNull() ?: 0.0,
                    comment.value
            )
        }, config).build()
    }

    class CommentsDataSourceFactory(private val gameId: Int, private val byRating: Boolean) : DataSource.Factory<Int, Game.Comment>() {
        override fun create(): DataSource<Int, Game.Comment> {
            return CommentsDataSource(gameId, byRating)
        }
    }
}
