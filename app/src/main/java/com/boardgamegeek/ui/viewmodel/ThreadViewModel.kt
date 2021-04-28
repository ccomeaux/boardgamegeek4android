package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.ThreadArticlesEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import java.lang.Exception

class ThreadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ForumRepository(application)
    private val _threadId = MutableLiveData<Int>()

    fun setThreadId(id: Int) {
        if (_threadId.value != id) _threadId.value = id
    }

    val articles: LiveData<RefreshableResource<ThreadArticlesEntity>> = _threadId.switchMap { id ->
        liveData {
            emit(when (id) {
                BggContract.INVALID_ID -> RefreshableResource.error("Invalid thread ID.")
                else -> {
                    try {
                        RefreshableResource.success(repository.loadThread(id))
                    } catch (e: Exception) {
                        RefreshableResource.error(e, application)
                    }
                }
            })
        }
    }
}
