package com.boardgamegeek

import android.os.Handler
import android.os.Looper

import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val NETWORK_THREAD_COUNT = 3

class AppExecutors internal constructor(val diskIO: Executor, val networkIO: Executor, val mainThread: Executor) {

    constructor() : this(Executors.newSingleThreadExecutor(),
            Executors.newFixedThreadPool(NETWORK_THREAD_COUNT),
            MainThreadExecutor())

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}