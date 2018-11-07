@file:JvmName("TaskUtils")

package com.boardgamegeek.extensions

import android.os.AsyncTask

//@JvmStatic
@SafeVarargs
fun <T> AsyncTask<T, *, *>.executeAsyncTask(vararg params: T) {
    executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, *params)
}