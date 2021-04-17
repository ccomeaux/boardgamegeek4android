package com.boardgamegeek.livedata

import android.database.ContentObserver
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import kotlinx.coroutines.*

open class RegisteredLiveDataCoroutine<T>(val application: BggApplication,
                                          val uri: Uri,
                                          private val notifyForDescendants: Boolean = false,
                                          private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
                                          private val loadData: suspend () -> T?) : MutableLiveData<T>() {
    private val contentObserver = Observer()

    override fun onActive() {
        super.onActive()
        updateData()
        application.contentResolver.registerContentObserver(uri, notifyForDescendants, contentObserver)
    }

    override fun onInactive() {
        super.onInactive()
        application.contentResolver.unregisterContentObserver(contentObserver)
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            updateData()
        }
    }

    private fun updateData() {
        scope.launch {
            withContext(Dispatchers.IO) {
                postValue(loadData())
            }
        }
    }
}
