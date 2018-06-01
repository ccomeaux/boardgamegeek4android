package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.database.ContentObserver
import android.net.Uri
import com.boardgamegeek.BggApplication

open class RegisteredLiveData<T>(val application: BggApplication,
                                 val uri: Uri,
                                 private val notifyForDescendants: Boolean = false,
                                 private val loadData: () -> T?) : MutableLiveData<T>() {
    private val contentObserver = Observer()

    override fun onActive() {
        super.onActive()
        application.appExecutors.diskIO.execute {
            postValue(loadData())
        }
        application.contentResolver.registerContentObserver(uri, notifyForDescendants, contentObserver)
    }

    override fun onInactive() {
        super.onInactive()
        application.contentResolver.unregisterContentObserver(contentObserver)
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            application.appExecutors.diskIO.execute {
                postValue(loadData())
            }
        }
    }
}