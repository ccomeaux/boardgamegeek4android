package com.boardgamegeek.livedata

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication

abstract class CalculatingListLoader<T>(val application: BggApplication) {
    private val result = CancelableMediatorLiveData<List<T>>()

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    init {
        _progress.value = 0 to 0
        @Suppress("LeakingThis")
        val dbSource = loadFromDatabase()
        result.addSource(dbSource) { list ->
            setValue(list ?: emptyList())
            if (shouldCalculate(list)) {
                result.removeSource(dbSource)
                application.appExecutors.diskIO.execute {
                    val sortedList = sortList(list)
                    sortedList?.forEachIndexed { index, item ->
                        if (result.shouldCancel) {
                            _progress.postValue(0 to 0)
                            return@execute
                        }
                        _progress.postValue(index to sortedList.size)
                        calculate(item)
                    }
                    application.appExecutors.mainThread.execute {
                        _progress.value = 0 to 0
                        result.addSource(dbSource) { newValue ->
                            setValue(newValue)
                        }
                        finishCalculating()
                    }
                }
            }
        }
    }

    fun asLiveData() = result as LiveData<List<T>>

    @MainThread
    private fun setValue(newValue: List<T>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    @MainThread
    protected abstract fun loadFromDatabase(): LiveData<List<T>>

    @MainThread
    protected open fun shouldCalculate(data: List<T>?): Boolean = true

    @WorkerThread
    protected open fun sortList(data: List<T>?): List<T>? = data

    @WorkerThread
    protected abstract fun calculate(data: T)

    @MainThread
    protected open fun finishCalculating() {
    }

    class CancelableMediatorLiveData<T> : MediatorLiveData<T>() {
        var shouldCancel = false

        override fun onInactive() {
            super.onInactive()
            shouldCancel = true
        }
    }
}
