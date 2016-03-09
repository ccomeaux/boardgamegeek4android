package com.boardgamegeek;

import android.app.Application;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;

import com.boardgamegeek.util.CrashReportingTree;
import com.boardgamegeek.util.VersionUtils;
import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;

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
			enableStrictMode();
		} else {
			Fabric.with(this, new Crashlytics());
			Timber.plant(new CrashReportingTree());
		}
		LeakCanary.install(this);
	}

	private void enableStrictMode() {
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectAll()
			.penaltyLog()
			.build());
		Builder builder = new Builder()
			.detectAll()
			.penaltyLog();
		if (VersionUtils.hasHoneycomb()) {
			builder.penaltyFlashScreen();
		} else {
			builder.penaltyDialog();
		}
		StrictMode.setThreadPolicy(builder.build());
	}
}
