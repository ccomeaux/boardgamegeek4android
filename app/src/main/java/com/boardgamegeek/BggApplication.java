package com.boardgamegeek;

import android.app.Application;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.text.TextUtils;

import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.events.BggEventBusIndex;
import com.boardgamegeek.util.CrashReportingTree;
import com.boardgamegeek.util.HttpUtils;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.stetho.Stetho;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import hugo.weaving.DebugLog;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class BggApplication extends Application {
	@Override
	@DebugLog
	public void onCreate() {
		super.onCreate();
		initializeFabric();
		if (BuildConfig.DEBUG) {
			Timber.plant(new DebugTree());
			enableStrictMode();
			Stetho.initialize(
				Stetho.newInitializerBuilder(this)
					.enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
					.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
					.build());
		} else {
			String username = AccountUtils.getUsername(this);
			if (!TextUtils.isEmpty(username)) {
				Crashlytics.setUserIdentifier(username);
			}
			Crashlytics.setString("BUILD_TIME", BuildConfig.BUILD_TIME);
			Crashlytics.setString("GIT_SHA", BuildConfig.GIT_SHA);
			Timber.plant(new CrashReportingTree());
		}
		LeakCanary.install(this);

		EventBus.builder()
			.logNoSubscriberMessages(BuildConfig.DEBUG)
			.throwSubscriberException(BuildConfig.DEBUG)
			.addIndex(new BggEventBusIndex())
			.installDefaultEventBus();

		Picasso.setSingletonInstance(new Picasso.Builder(this)
			.downloader(new OkHttp3Downloader(HttpUtils.getHttpClientWithCache(this)))
			.build());
	}

	private void initializeFabric() {
		final Crashlytics crashlytics = new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build();
		Fabric.with(this, crashlytics, new Answers());
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
