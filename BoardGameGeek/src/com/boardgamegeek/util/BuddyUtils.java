package com.boardgamegeek.util;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class BuddyUtils {
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	public static final String KEY_BUDDY_FULL_NAME = "BUDDY_FULL_NAME";

	public static String buildFullName(Cursor cursor, int firstNameIndex, int lastNameIndex) {
		String firstName = cursor.getString(firstNameIndex);
		String lastName = cursor.getString(lastNameIndex);
		return buildFullName(firstName, lastName);
	}

	public static String buildFullName(String firstName, String lastName) {
		if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
			return "";
		} else if (TextUtils.isEmpty(firstName)) {
			return lastName.trim();
		} else if (TextUtils.isEmpty(lastName)) {
			return firstName.trim();
		} else {
			return firstName.trim() + " " + lastName.trim();
		}
	}

	public static String getNameFromIntent(Intent intent) {
		Uri uri = intent.getData();
		if ("http".equals(uri.getScheme())) {
			boolean usernameIsNext = false;
			for (String path : uri.getPathSegments()) {
				if (usernameIsNext) {
					return  path;
				} else if ("user".equals(path)) {
					usernameIsNext = true;
				}
			}
		} else {
			return intent.getStringExtra(BuddyUtils.KEY_BUDDY_NAME);
		}
		return "";
	}
}
