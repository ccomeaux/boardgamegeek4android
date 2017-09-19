package com.boardgamegeek;

import android.app.Application;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.os.StrictMode.VmPolicy;
import android.text.TextUtils;

import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.events.BggEventBusIndex;
import com.boardgamegeek.util.CrashReportingTree;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
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
				Crashlytics.setUserIdentifier(String.valueOf(username.hashCode()));
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

		if (VERSION.SDK_INT >= VERSION_CODES.O)
			NotificationUtils.createNotificationChannels(getApplicationContext());
	}

	private void initializeFabric() {
		final Crashlytics crashlytics = new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build();
		Fabric.with(this, crashlytics, new Answers());
	}

	private void enableStrictMode() {
		final VmPolicy.Builder builder = new VmPolicy.Builder()
			.detectActivityLeaks()
			.detectLeakedClosableObjects()
			.detectLeakedSqlLiteObjects()
			.penaltyLog();
		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
			builder.detectFileUriExposure().detectLeakedRegistrationObjects();
		}
		if (VERSION.SDK_INT >= VERSION_CODES.M) {
			builder.detectCleartextNetwork();
		}
		StrictMode.setVmPolicy(builder.build());
		StrictMode.setThreadPolicy(new Builder()
			.detectAll()
			.penaltyLog()
			.penaltyFlashScreen()
			.build());
	}
}
