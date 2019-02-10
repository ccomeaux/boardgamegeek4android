package com.boardgamegeek.livedata

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.util.NetworkUtils
import org.xmlpull.v1.XmlPullParserException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class NetworkLoader<T, U>(val application: BggApplication) {
    private val result = MediatorLiveData<RefreshableResource<T>>()

    init {
        val liveData = AbsentLiveData.create<T>()
        result.addSource(liveData) { newData ->
            result.removeSource(liveData)
            if (NetworkUtils.isOffline(application)) {
                setValue(RefreshableResource.error(application.getString(R.string.msg_offline), newData))
            } else {
                setValue(RefreshableResource.refreshing(newData))
            }
            if (NetworkUtils.isOffline(application)) return@addSource
            createCall().enqueue(object : Callback<U> {
                override fun onResponse(call: Call<U>?, response: Response<U>?) {
                    if (response?.isSuccessful == true) {
                        val body = response.body()
                        if (body != null) {
                            application.appExecutors.diskIO.execute {
                                val parsedResult = parseResult(body)
                                onRefreshSucceeded()
                                application.appExecutors.mainThread.execute {
                                    setValue(RefreshableResource.success(parsedResult))
                                }
                            }
                        } else {
                            onRefreshFailed()
                            setValue(RefreshableResource.error(application.getString(R.string.msg_update_invalid_response, application.getString(typeDescriptionResId)), null))
                        }
                    } else {
                        onRefreshFailed()
                        setValue(RefreshableResource.error(getHttpErrorMessage(response), null))
                    }
                }

                override fun onFailure(call: Call<U>?, t: Throwable?) {
                    onRefreshFailed()

                    if (t is RuntimeException && t.cause is XmlPullParserException) {
                        setValue(RefreshableResource.error(application.getString(R.string.parse_error), null))
                    } else {
                        setValue(RefreshableResource.error(t, null))
                    }
                }
            })
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
    protected abstract fun createCall(): Call<U>

    @WorkerThread
    protected abstract fun parseResult(result: U): T

    @WorkerThread
    protected open fun onRefreshSucceeded() {
    }

    @WorkerThread
    protected open fun onRefreshFailed() {
    }

    private fun getHttpErrorMessage(response: Response<U>?): String {
        return when {
            response == null -> application.getString(R.string.msg_sync_error_null)
            response.code() >= 500 -> application.getString(R.string.msg_sync_response_500, response.code().toString())
            response.code() == 429 -> application.getString(R.string.msg_sync_response_429)
            response.code() == 429 -> application.getString(R.string.msg_sync_response_202)
            response.message().isNotBlank() -> response.message()
            else -> application.getString(R.string.msg_sync_error_http_code, response.code().toString())
        }
    }
}