package com.boardgamegeek.io

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(context: Context, dispatcher: CoroutineDispatcher = Dispatchers.IO, apiCall: suspend () -> T): Result<T> {
    return withContext(dispatcher) {
        try {
            Result.success(apiCall.invoke())
        } catch (throwable: Throwable) {
            val errorMessage = when (throwable) {
                is SocketTimeoutException -> context.getString(R.string.msg_sync_error_timeout)
                is IOException -> context.getString(R.string.msg_sync_error)
                is HttpException -> {
                    @StringRes val resId: Int = when {
                        throwable.code() >= 500 -> R.string.msg_sync_response_500
                        throwable.code() == 429 -> R.string.msg_sync_response_429
                        throwable.code() == 202 -> R.string.msg_sync_response_202
                        else -> R.string.msg_sync_error_http_code
                    }
                    context.getString(resId, throwable.code().toString())
                }
                else -> throwable.localizedMessage
            }
            Result.failure(Exception(errorMessage))
        }
    }
}
