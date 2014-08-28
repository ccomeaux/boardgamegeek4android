package com.boardgamegeek.util;

import android.database.Cursor;
import android.text.TextUtils;

public class BuddyUtils {
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";

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
}
