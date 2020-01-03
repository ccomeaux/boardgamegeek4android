package com.boardgamegeek.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * A [timber.log.Timber.Tree] that reports crashes to Fabric.
 */
class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }
        Crashlytics.getInstance().core.log(priority, tag, message)
        if (t != null && priority == Log.ERROR) {
            Crashlytics.getInstance().core.logException(t)
        }
    }
}