package com.boardgamegeek.util;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

/**
 * A {@link timber.log.Timber.Tree} that reports crashes to Crashlytics.
 */
public class CrashReportingTree extends Timber.HollowTree {
	@Override
	public void w(String message, Object... args) {
		Crashlytics.log(String.format(message, args));
	}

	@Override
	public void w(Throwable t, String message, Object... args) {
		w(message, args);
	}

	@Override
	public void e(String message, Object... args) {
		Crashlytics.log(String.format("ERROR: " + message, args));
	}

	@Override
	public void e(Throwable t, String message, Object... args) {
		e(message, args);
		Crashlytics.logException(t);
	}
}
