package com.boardgamegeek.tasks.sync

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asHttpErrorMessage
import com.boardgamegeek.extensions.isOffline
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.util.RemoteConfig
import com.boardgamegeek.util.RemoteConfig.Companion.getBoolean
import retrofit2.Call
import timber.log.Timber

abstract class SyncTask<T> internal constructor(context: Context) : AsyncTask<Void, Void, String>() {
    @SuppressLint("StaticFieldLeak")
    protected val context: Context = context.applicationContext

    protected val startTime: Long = System.currentTimeMillis()

    protected var bggService: BggService? = null

    private var call: Call<T>? = null

    protected var currentPage = 1
        private set

    private val errorMessageLiveData = MutableLiveData<String>()

    override fun doInBackground(vararg params: Void): String {
        bggService = createService()
        if (!isRequestParamsValid) return context.getString(R.string.msg_update_invalid_request, context.getString(typeDescriptionResId))
        if (context.isOffline()) return context.getString(R.string.msg_offline)
        if (!getBoolean(RemoteConfig.KEY_SYNC_ENABLED)) return context.getString(R.string.msg_sync_remotely_disabled)
        try {
            var shouldContinue = false
            currentPage = 0
            do {
                currentPage++
                call = createCall()
                val response = call?.execute()
                if (response == null) {
                    Timber.w("Null response while syncing %s.", context.getString(typeDescriptionResId))
                } else {
                    if (response.isSuccessful) {
                        if (isResponseBodyValid(response.body())) {
                            persistResponse(response.body())
                        } else {
                            return context.getString(R.string.msg_update_invalid_response,
                                    context.getString(typeDescriptionResId))
                        }
                    } else {
                        Timber.w("Received response %s while syncing %s.", response.code(), context.getString(typeDescriptionResId))
                        return response.code().asHttpErrorMessage(context)
                    }
                    shouldContinue = hasMorePages(response.body())
                }
                if (isCancelled) break
            } while (shouldContinue)
            if (!isCancelled) finishSync()
        } catch (e: Exception) {
            Timber.w(e, "Exception fetching %1\$s: %2\$s", context.getString(typeDescriptionResId), e.message)
            return e.localizedMessage ?: e.message ?: e.toString()
        }
        return ""
    }

    override fun onPostExecute(errorMessage: String) {
        Timber.w(errorMessage)
        errorMessageLiveData.postValue(errorMessage)
    }

    override fun onCancelled() {
        call?.cancel()
    }

    @get:StringRes
    protected abstract val typeDescriptionResId: Int

    protected abstract fun createCall(): Call<T>?

    protected open fun createService(): BggService {
        return Adapter.createForXml()
    }

    protected abstract val isRequestParamsValid: Boolean

    open fun isResponseBodyValid(body: T?): Boolean {
        return body != null
    }

    protected abstract fun persistResponse(body: T?)

    protected open fun hasMorePages(body: T?): Boolean {
        return false
    }

    protected open fun finishSync() {}
}