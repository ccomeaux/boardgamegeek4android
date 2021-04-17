package com.boardgamegeek.livedata

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.core.os.postAtTime
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.util.RateLimiter
import java.util.concurrent.TimeUnit

open class RegisteredLiveData<T>(val application: BggApplication,
                                 val uri: Uri,
                                 private val notifyForDescendants: Boolean = false,
                                 throttleTimeout: Int = 1,
                                 throttleTimeUnit: TimeUnit = TimeUnit.SECONDS,
                                 private val loadData: () -> T?) : MutableLiveData<T>() {
    private val contentObserver = Observer()
    private val rateLimiter = RateLimiter<Int>(throttleTimeout, throttleTimeUnit)
    val handler = Handler()
    private val key = 0
    private val token = "RegisteredLiveData"

    override fun onActive() {
        super.onActive()
        updateData()
        application.contentResolver.registerContentObserver(uri, notifyForDescendants, contentObserver)
    }

    override fun onInactive() {
        super.onInactive()
        application.contentResolver.unregisterContentObserver(contentObserver)
        rateLimiter.reset(key)
    }

    internal inner class Observer : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            if (rateLimiter.shouldProcess(key)) {
                updateData()
            } else {
                handler.removeCallbacksAndMessages(token)
                handler.postAtTime(rateLimiter.willProcessAt(key), token) { updateData() }
            }
        }
    }

    private fun updateData() {
        application.appExecutors.diskIO.execute {
            postValue(loadData())
            rateLimiter.reset(key)
        }
    }
}
