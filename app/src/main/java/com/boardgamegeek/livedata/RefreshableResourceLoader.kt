package com.boardgamegeek.livedata

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.content.Context
import android.support.annotation.MainThread
import android.support.annotation.StringRes
import android.support.annotation.WorkerThread
import com.boardgamegeek.R
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class RefreshableResourceLoader<T, U>(val context: Context) {
    private val result = MediatorLiveData<RefreshableResource<T>>()

    fun load(): LiveData<RefreshableResource<T>> {
        if (!isRequestParamsValid()) {
            result.value = RefreshableResource.error(context.getString(com.boardgamegeek.R.string.msg_update_invalid_request, context.getString(typeDescriptionResId)))
        } else {
            val dbSource = loadFromDatabase()
            result.addSource(dbSource) { data ->
                result.removeSource(dbSource)
                if (shouldRefresh(data)) {
                    refresh(dbSource)
                } else {
                    result.addSource(dbSource) { newData -> result.setValue(RefreshableResource.success(newData)) }
                }
            }
        }
        return result
    }

    protected abstract val typeDescriptionResId: Int

    @MainThread
    protected open fun isRequestParamsValid(): Boolean {
        return true
    }

    @MainThread
    protected abstract fun loadFromDatabase(): LiveData<T>

    @MainThread
    protected abstract fun shouldRefresh(data: T?): Boolean

    fun refresh() {
        refresh(loadFromDatabase())
    }

    private fun refresh(dbSource: LiveData<T>) {
        result.addSource(dbSource) { newData ->
            if (NetworkUtils.isOffline(context)) {
                result.value = RefreshableResource.error(context.getString(R.string.msg_offline), newData)
            } else {
                result.setValue(RefreshableResource.refreshing(newData))
            }
        }
        if (NetworkUtils.isOffline(context)) return
        createCall().enqueue(object : Callback<U> {
            override fun onResponse(call: Call<U>?, response: Response<U>?) {
                if (response?.isSuccessful == true) {
                    if (isResponseBodyValid(response.body())) {
                        saveCallResult(response.body()!!)
                        result.removeSource(dbSource)
                        result.addSource(dbSource) { newData ->
                            result.setValue(RefreshableResource.success(newData))
                        }
                    } else {
                        result.removeSource(dbSource)
                        result.addSource(dbSource) { newData ->
                            result.setValue(RefreshableResource.error(context.getString(R.string.msg_update_invalid_response, context.getString(typeDescriptionResId)), newData))
                        }
                    }
                } else {
                    result.addSource(dbSource) { newData ->
                        result.removeSource(dbSource)
                        result.setValue(RefreshableResource.error(getHttpErrorMessage(response?.code()
                                ?: 500), newData))
                    }
                }
            }

            override fun onFailure(call: Call<U>?, t: Throwable?) {
                result.addSource(dbSource) { newData ->
                    result.setValue(RefreshableResource.error(t, newData))
                }
            }
        })
    }

    @MainThread
    protected abstract fun createCall(): Call<U>

    protected fun isResponseBodyValid(body: U?): Boolean {
        return body != null
    }

    private fun getHttpErrorMessage(httpCode: Int): String {
        @StringRes val resId: Int = when {
            httpCode >= 500 -> R.string.msg_sync_response_500
            httpCode == 429 -> R.string.msg_sync_response_429
            else -> R.string.msg_sync_error_http_code
        }
        return context.getString(resId, httpCode.toString())
    }

    @WorkerThread
    protected abstract fun saveCallResult(item: U)
}