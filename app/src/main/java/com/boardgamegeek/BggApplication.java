package com.boardgamegeek;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.os.StrictMode.VmPolicy;

import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.events.BggEventBusIndex;
import com.boardgamegeek.extensions.PreferenceUtils;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.util.CrashReportingTree;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.RemoteConfig;
import com.facebook.stetho.Stetho;
import com.google.firebase.iid.FirebaseInstanceId;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.Set;

import androidx.multidex.MultiDexApplication;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class BggApplication extends MultiDexApplication {
	private AppExecutors appExecutors;

	@Override
	public void onCreate() {
		super.onCreate();
		appExecutors = new AppExecutors();
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
			Timber.plant(new CrashReportingTree());
		}

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

		FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
			String deviceToken = instanceIdResult.getToken();
			Timber.i("Firebase token is %s", deviceToken);
		});
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
			.detectDiskWrites()
			.detectNetwork()
			.penaltyLog()
			.penaltyFlashScreen()
			.build());
	}

	private void migrateCollectionStatusSettings() {
		Set<String> set = PreferenceUtils.getSyncStatuses(this, null);
		if (set == null) {
			String[] oldSyncStatuses = PreferencesUtils.getOldSyncStatuses(getApplicationContext());
			if (oldSyncStatuses != null && oldSyncStatuses.length > 0) {
				PreferenceUtils.setSyncStatuses(getApplicationContext(), oldSyncStatuses);
			}
		}
	}

	public AppExecutors getAppExecutors() {
		return appExecutors;
	}
}
