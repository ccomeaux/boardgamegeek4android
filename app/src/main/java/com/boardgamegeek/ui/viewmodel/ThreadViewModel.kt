package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.ThreadArticlesEntity
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ThreadRepository

class ThreadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ThreadRepository(getApplication())
    private val _threadId = MutableLiveData<Int>()

    fun setThreadId(id: Int) {
        if (_threadId.value != id) _threadId.value = id
    }

    val articles: LiveData<RefreshableResource<ThreadArticlesEntity>> = Transformations.switchMap(_threadId) { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> repository.getThread(id)
        }
    }
}