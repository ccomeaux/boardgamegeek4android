package com.boardgamegeek.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AccountUtils {
	private static final String KEY_PREFIX = "account_";
	private static final String KEY_USERNAME = KEY_PREFIX + "username";
	private static final String KEY_FULL_NAME = KEY_PREFIX + "full_name";
	private static final String KEY_AVATAR_URL = KEY_PREFIX + "avatar_url";

	private AccountUtils() {
	}

	public static void setUsername(final Context context, final String username) {
		setString(context, username, KEY_USERNAME);
	}

	public static void setFullName(final Context context, String fullName) {
		setString(context, fullName, KEY_FULL_NAME);
	}

	public static void setAvatarUrl(final Context context, String avatarUrl) {
		setString(context, avatarUrl, KEY_AVATAR_URL);
	}

	public static String getUsername(final Context context) {
		return getString(context, KEY_USERNAME);
	}

	public static String getFullName(final Context context) {
		return getString(context, KEY_FULL_NAME);
	}

	public static String getAvatarUrl(final Context context) {
		return getString(context, KEY_AVATAR_URL);
	}

	private static void setString(Context context, String value, String key) {
		SharedPreferences sp = getSharedPreferences(context);
		sp.edit().putString(key, value).apply();
	}

	private static String getString(Context context, String key) {
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(key, null);
	}

	private static SharedPreferences getSharedPreferences(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
}
