package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.ThreadArticles
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    application: Application,
    private val repository: ForumRepository,
) : AndroidViewModel(application) {
    private val _threadId = MutableLiveData<Int>()

    fun setThreadId(id: Int) {
        if (_threadId.value != id) _threadId.value = id
    }

    val articles: LiveData<RefreshableResource<ThreadArticles>> = _threadId.switchMap { id ->
        liveData {
            emit(
                when (id) {
                    BggContract.INVALID_ID -> RefreshableResource.error("Invalid thread ID.")
                    else -> {
                        try {
                            RefreshableResource.success(repository.loadThread(id))
                        } catch (e: Exception) {
                            RefreshableResource.error(e, application)
                        }
                    }
                }
            )
        }
    }
}
