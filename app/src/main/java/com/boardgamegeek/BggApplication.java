package com.boardgamegeek;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import hugo.weaving.DebugLog;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class BggApplication extends Application {
	@Override
	@DebugLog
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {
			Timber.plant(new DebugTree());
		} else {
			Fabric.with(this, new Crashlytics());
			//TODO: replace with Crashlytics tree
			Timber.plant(new Timber.HollowTree());
		}
	}
}
