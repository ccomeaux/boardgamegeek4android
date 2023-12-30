package com.boardgamegeek.util

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.RetentionManager

class DebugCrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler? = null,
    applicationContext: Context
) : Thread.UncaughtExceptionHandler {

    private val chuckerCollector: ChuckerCollector by lazy {
        ChuckerCollector(
            context = applicationContext,
            // Toggles visibility of the push notification
            showNotification = true,
            // Allows to customize the retention period of collected data
            retentionPeriod = RetentionManager.Period.FOREVER
        )
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        chuckerCollector.onError("error", e)
        defaultHandler?.uncaughtException(t, e)
    }
}
