package com.boardgamegeek.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class HelpUtils {
	public static final String HELP_HOME_KEY = "help.home";
	public static final String HELP_COLLECTION_KEY = "help.collection";
	public static final String HELP_SEARCHRESULTS_KEY = "help.searchresults";
	public static final String HELP_LOGPLAY_KEY = "help.logplay";
	public static final String HELP_COLORS_KEY = "help.colors";
	public static final String HELP_THREAD_KEY = "help.thread";

	public static boolean showHelp(Context context, String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		final int shownVersion = preferences.getInt(key, 0);
		return version > shownVersion;
	}

	public static boolean updateHelp(Context context, String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return preferences.edit().putInt(key, version).commit();
	}

	public static String getVersionName(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return pInfo.versionName;
		} catch (NameNotFoundException e) {
			return "?.?";
		}
	}
}
