package com.boardgamegeek.livedata

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication

abstract class DatabaseResourceLoader<T>(val application: BggApplication) {
    private val result = MediatorLiveData<T>()

    init {
        application.appExecutors.diskIO.execute {
            val dbSource = loadFromDatabase()
            result.addSource(dbSource) {
                result.postValue(it)
            }
        }
    }

    fun asLiveData() = result as LiveData<T>

    protected abstract fun loadFromDatabase(): LiveData<T>
}