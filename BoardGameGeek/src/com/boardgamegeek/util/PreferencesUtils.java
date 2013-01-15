package com.boardgamegeek.util;

import com.boardgamegeek.pref.ListPreferenceMultiSelect;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesUtils {
	private PreferencesUtils() {
	}

	public static boolean getExactSearch(Context context) {
		return getBoolean(context, "exactSearch", true);
	}

	public static boolean getSkipResults(Context context) {
		return getBoolean(context, "skipResults", true);
	}

	public static String[] getSyncStatuses(Context context) {
		return getStringArray(context, "syncStatuses", "");
	}

	public static boolean getSyncPlays(Context context) {
		return getBoolean(context, "syncPlays", false);
	}

	public static boolean getSyncBuddies(Context context) {
		return getBoolean(context, "syncBuddies", false);
	}

	public static boolean getShowSyncNotifications(Context context) {
		return getBoolean(context, "sync_notifications", false);
	}

	private static boolean getBoolean(Context context, String key, boolean defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(key, defaultValue);
	}

	private static String getString(Context context, String key, String defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(key, defaultValue);
	}

	private static String[] getStringArray(Context context, String key, String defaultValue) {
		return ListPreferenceMultiSelect.parseStoredValue(getString(context, key, defaultValue));
	}
}
