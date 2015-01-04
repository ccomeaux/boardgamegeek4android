package com.boardgamegeek;

import android.app.Application;

import hugo.weaving.DebugLog;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class BggApplication extends Application {

	@Override
	@DebugLog
	public void onCreate() {
		if (BuildConfig.DEBUG) {
			Timber.plant(new DebugTree());
		} else {
			Timber.plant(new Timber.HollowTree());
		}
	}
}
