package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.database.ContentObserver
import android.net.Uri

open class RegisteredLiveData<T>(val context: Context, val uri: Uri, private val loadData: () -> T?) : MutableLiveData<T>() {
    private val contentObserver = Observer()

    override fun onActive() {
        super.onActive()
        postValue(loadData())
        context.contentResolver.registerContentObserver(uri, false, contentObserver)
    }

    override fun onInactive() {
        super.onInactive()
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            postValue(loadData())
        }
    }
}