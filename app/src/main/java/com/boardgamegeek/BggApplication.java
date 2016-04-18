package com.boardgamegeek;

import android.app.Application;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;

import com.boardgamegeek.util.CrashReportingTree;
import com.boardgamegeek.util.HttpUtils;
import com.crashlytics.android.Crashlytics;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;

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

		Picasso.setSingletonInstance(new Picasso.Builder(this)
			.downloader(new OkHttp3Downloader(HttpUtils.getHttpClientWithCache(this)))
			.build());
	}

	private void enableStrictMode() {
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectAll()
			.penaltyLog()
			.build());
		Builder builder = new Builder()
			.detectAll()
			.penaltyLog();
		builder.penaltyFlashScreen();
		StrictMode.setThreadPolicy(builder.build());
	}
}
