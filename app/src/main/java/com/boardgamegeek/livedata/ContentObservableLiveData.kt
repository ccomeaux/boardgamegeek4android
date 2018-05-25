package com.boardgamegeek.livedata

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.database.ContentObserver
import android.net.Uri

abstract class ContentObservableLiveData<T>(val context: Context) : MutableLiveData<T>() {
    private val contentObserver = Observer()

    override fun onActive() {
        super.onActive()
        registerContentObserver()
    }

    override fun onInactive() {
        super.onInactive()
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    fun load(): ContentObservableLiveData<T> {
        registerContentObserver()
        loadData()
        return this
    }

    private fun registerContentObserver() {
        context.contentResolver.registerContentObserver(uri, false, contentObserver)
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }

    protected abstract var uri: Uri

    protected abstract fun loadData()
}