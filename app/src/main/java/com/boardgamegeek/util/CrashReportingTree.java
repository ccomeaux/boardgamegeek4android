package com.boardgamegeek.util;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

/**
 * A {@link timber.log.Timber.Tree} that reports crashes to Crashlytics.
 */
public class CrashReportingTree extends Timber.Tree {
	@Override
	protected void log(int priority, String tag, String message, Throwable t) {
		if (priority == Log.VERBOSE || priority == Log.DEBUG) {
			return;
		}

		Crashlytics.getInstance().core.log(priority, tag, message);

		if (t != null) {
			Crashlytics.getInstance().core.logException(t);
		}
	}
}
