package com.boardgamegeek;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.os.StrictMode.VmPolicy;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.events.BggEventBusIndex;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.util.CrashReportingTree;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.RemoteConfig;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.stetho.Stetho;
import com.google.firebase.iid.FirebaseInstanceId;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.Set;

import hugo.weaving.DebugLog;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class BggApplication extends MultiDexApplication {
	private AppExecutors appExecutors;

	@Override
	@DebugLog
	public void onCreate() {
		super.onCreate();
		appExecutors = new AppExecutors();
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

		RemoteConfig.init();

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

		migrateCollectionStatusSettings();
		SyncPrefs.migrate(this);

		String refreshedToken = FirebaseInstanceId.getInstance().getToken();
		Timber.i("Refreshed Firebase token to %s", refreshedToken);
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

	private void migrateCollectionStatusSettings() {
		Set<String> set = PreferencesUtils.getSyncStatuses(this, null);
		if (set == null) {
			String[] oldSyncStatuses = PreferencesUtils.getOldSyncStatuses(getApplicationContext());
			if (oldSyncStatuses != null && oldSyncStatuses.length > 0) {
				PreferencesUtils.setSyncStatuses(getApplicationContext(), oldSyncStatuses);
			}
		}
	}

	public AppExecutors getAppExecutors() {
		return appExecutors;
	}
}
