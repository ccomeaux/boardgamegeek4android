package com.boardgamegeek;

import com.boardgamegeek.pref.ListPreferenceMultiSelect;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

public class BggApplication extends Application {
	private final static String TAG = "BggApplication";

	public final static String siteUrl = "http://www.boardgamegeek.com/";
	public static String HELP_BOARDGAME_KEY = "help.boardgame";
	public static String HELP_COLLECTION_KEY = "help.collection";
	public static String HELP_SEARCHRESULTS_KEY = "help.searchresults";
	public static String HELP_LOGPLAY_KEY = "help.logplay";

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
			Log.e(TAG, "NameNotFoundException in getVersion", e);
		}
		return "";
	}

	public String getUserName() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getString("username", "");
	}

	public String getPassword() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getString("password", "");
	}

	public boolean getImageLoad() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("imageLoad", true);
	}

	public boolean getExactSearch() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("exactSearch", true);
	}

	public boolean getSkipResults() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("skipResults", true);
	}

	public String[] getSyncStatuses() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String statuses = preferences.getString("syncStatuses", "");
		return ListPreferenceMultiSelect.parseStoredValue(statuses);
	}

	public boolean getSyncBuddies() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("syncBuddies", true);
	}

	public boolean getPlayLoggingHideMenu() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideLog", false);
	}

	public boolean getPlayLoggingHideQuickMenu() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideQuickLog", false);
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

	public boolean getPlayLoggingHidePlayerTeamColor() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideTeamColor", false);
	}

	public boolean getPlayLoggingHidePlayerPosition() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHidePosition", false);
	}

	public boolean getPlayLoggingHidePlayerScore() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideScore", false);
	}

	public boolean getPlayLoggingHidePlayerRating() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideRating", false);
	}

	public boolean getPlayLoggingHidePlayerNew() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideNew", false);
	}

	public boolean getPlayLoggingHidePlayerWin() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("logHideWin", false);
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
