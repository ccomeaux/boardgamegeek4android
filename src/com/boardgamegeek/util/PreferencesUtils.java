package com.boardgamegeek.util;

import com.boardgamegeek.pref.ListPreferenceMultiSelect;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesUtils {
	private PreferencesUtils() {
	}

	public static String[] getSyncStatuses(Context context) {
		return getStringArray(context, "syncStatuses", "");
	}

	public static boolean getShowSyncNotifications(Context context) {
		return getBoolean(context, "sync_notifications", false);
	}

	public static boolean getBoolean(Context context, String key, boolean defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(key, defaultValue);
	}

	public static long getLong(Context context, String key, long defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getLong(key, defaultValue);
	}

	public static String getString(Context context, String key, String defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(key, defaultValue);
	}

	public static String[] getStringArray(Context context, String key, String defaultValue) {
		return ListPreferenceMultiSelect.parseStoredValue(getString(context, key, defaultValue));
	}
}
