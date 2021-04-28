package com.boardgamegeek.entities

import android.app.Application
import androidx.annotation.StringRes
import com.boardgamegeek.R
import retrofit2.HttpException
import java.net.SocketTimeoutException

enum class Status {
    SUCCESS,
    ERROR,
    REFRESHING
}

data class RefreshableResource<out T>(val status: Status, val data: T?, val message: String = "") {
    companion object {
        fun <T> success(data: T?): RefreshableResource<T> {
            return RefreshableResource(Status.SUCCESS, data)
        }

        fun <T> error(msg: String, data: T? = null): RefreshableResource<T> {
            return RefreshableResource(Status.ERROR, data, msg)
        }

        fun <T> error(t: Throwable?, application: Application, data: T? = null): RefreshableResource<T> {
            val message = when (t) {
                is HttpException -> {
                    @StringRes val resId: Int = when {
                        t.code() >= 500 -> R.string.msg_sync_response_500
                        t.code() == 429 -> R.string.msg_sync_response_429
                        t.code() == 202 -> R.string.msg_sync_response_202
                        else -> R.string.msg_sync_error_http_code
                    }
                    application.getString(resId, t.code().toString())
                }
                is SocketTimeoutException -> application.getString(R.string.msg_sync_error_timeout)
                else -> t?.localizedMessage ?: application.getString(R.string.msg_sync_error)
            }
            return RefreshableResource(Status.ERROR, data, message)
        }

        fun <T> refreshing(data: T? = null): RefreshableResource<T> {
            return RefreshableResource(Status.REFRESHING, data)
        }

        fun <T> map(source: RefreshableResource<*>, data: T? = null): RefreshableResource<T> {
            return RefreshableResource(source.status, data, source.message)
        }
    }
}
