package com.boardgamegeek.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * A [timber.log.Timber.Tree] that reports crashes to Crashlytics.
 */
class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }
        FirebaseCrashlytics.getInstance().log(message)
        if (t != null && priority == Log.ERROR) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}