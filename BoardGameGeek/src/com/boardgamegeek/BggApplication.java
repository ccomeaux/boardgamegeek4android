package com.boardgamegeek;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class BggApplication extends Application {
	private static final String TAG = makeLogTag(BggApplication.class);

	public final static String siteUrl = "http://www.boardgamegeek.com/";
	public static final String HELP_COLLECTION_KEY = "help.collection";
	public static final String HELP_SEARCHRESULTS_KEY = "help.searchresults";
	public static final String HELP_LOGPLAY_KEY = "help.logplay";
	public static final String HELP_COLORS_KEY = "help.colors";

	public static final String AUTHTOKEN_TYPE = "com.boardgamegeek";
	public static final String ACCOUNT_TYPE = "com.boardgamegeek";

	private static BggApplication singleton;

	public static BggApplication getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
	}

	public static String getVersionDescription(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return "Version " + pInfo.versionName;
		} catch (NameNotFoundException e) {
			LOGE(TAG, "NameNotFoundException in getVersion", e);
		}
		return "";
	}

	public boolean getPlayLoggingHideLength() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideLength", false);
	}

	public boolean getPlayLoggingHideLocation() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideLocation", false);
	}

	public boolean getPlayLoggingHideIncomplete() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideIncomplete", false);
	}

	public boolean getPlayLoggingHideNoWinStats() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideNoWinStats", false);
	}

	public boolean getPlayLoggingHideComments() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideComments", false);
	}

	public boolean getPlayLoggingHidePlayerList() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHidePlayerList", false);
	}

	public boolean getPlayLoggingEditPlayer() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logEditPlayer", false);
	}

	public boolean showHelp(String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final int shownVersion = preferences.getInt(key, 0);
		return version > shownVersion;
	}

	public boolean updateHelp(String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.edit().putInt(key, version).commit();
	}
}
