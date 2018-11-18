package com.boardgamegeek.livedata

import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.util.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class RefreshableResourceLoader<T, U>(val application: BggApplication) {
    private val result = MediatorLiveData<RefreshableResource<T>>()
    var page = 0

    init {
        @Suppress("LeakingThis")
        val dbSource = loadFromDatabase()
        result.addSource(dbSource) { data ->
            result.removeSource(dbSource)
            if (shouldRefresh(data)) {
                refresh(dbSource)
            } else {
                result.addSource(dbSource) { newData -> setValue(RefreshableResource.success(newData)) }
            }
        }
    }

    fun asLiveData() = result as LiveData<RefreshableResource<T>>

    @MainThread
    private fun setValue(newValue: RefreshableResource<T>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    protected abstract val typeDescriptionResId: Int

    @MainThread
    protected abstract fun loadFromDatabase(): LiveData<T>

    @MainThread
    protected abstract fun shouldRefresh(data: T?): Boolean

    private fun refresh(dbSource: LiveData<T>) {
        val isOffline = NetworkUtils.isOffline(application)
        result.addSource(dbSource) { newData ->
            if (isOffline) {
                setValue(RefreshableResource.error(application.getString(R.string.msg_offline), newData))
            } else {
                setValue(RefreshableResource.refreshing(newData))
            }
        }
        if (!isOffline) {
            page = 1
            makeCall(page, dbSource)
        }
    }

    @MainThread
    private fun makeCall(currentPage: Int, dbSource: LiveData<T>) {
        val call = createCall(currentPage)
        call.enqueue(object : Callback<U> {
            override fun onResponse(call: Call<U>?, response: Response<U>?) {
                result.removeSource(dbSource)
                if (response?.isSuccessful == true) {
                    val body = response.body()
                    if (body != null) {
                        application.appExecutors.diskIO.execute {
                            saveCallResult(body)
                            application.appExecutors.mainThread.execute {
                                if (hasMorePages(body)) {
                                    result.addSource(loadFromDatabase()) { newData ->
                                        setValue(RefreshableResource.refreshing(newData))
                                    }
                                    page++
                                    makeCall(page, dbSource)
                                } else {
                                    application.appExecutors.diskIO.execute { onRefreshSucceeded() }
                                    result.addSource(loadFromDatabase()) { newData ->
                                        setValue(RefreshableResource.success(newData))
                                    }
                                }
                            }
                        }
                    } else {
                        application.appExecutors.diskIO.execute { onRefreshFailed() }
                        result.addSource(dbSource) { newData ->
                            setValue(RefreshableResource.error(application.getString(R.string.msg_update_invalid_response, application.getString(typeDescriptionResId)), newData))
                        }
                    }
                } else {
                    application.appExecutors.diskIO.execute { onRefreshFailed() }
                    result.addSource(dbSource) { newData ->
                        setValue(RefreshableResource.error(getHttpErrorMessage(response?.code() ?: 500), newData))
                    }
                }
            }

            override fun onFailure(call: Call<U>?, t: Throwable?) {
                result.removeSource(dbSource)
                application.appExecutors.diskIO.execute { onRefreshFailed() }
                result.addSource(dbSource) { newData ->
                    setValue(RefreshableResource.error(t, newData))
                }
            }
        })
    }

    @MainThread
    protected abstract fun createCall(page: Int): Call<U>

    @WorkerThread
    protected abstract fun saveCallResult(result: U)

    @MainThread
    protected open fun hasMorePages(result: U) = false

    @WorkerThread
    protected open fun onRefreshSucceeded() {
    }

    @WorkerThread
    protected open fun onRefreshFailed() {
    }

    private fun getHttpErrorMessage(httpCode: Int): String {
        @StringRes val resId: Int = when {
            httpCode >= 500 -> R.string.msg_sync_response_500
            httpCode == 429 -> R.string.msg_sync_response_429
            else -> R.string.msg_sync_error_http_code
        }
        return application.getString(resId, httpCode.toString())
    }
}