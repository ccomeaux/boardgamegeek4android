package com.boardgamegeek.util;

import android.text.TextUtils;

public class DataUtils {
	public static String generatePollResultsKey(int level, String value) {
		if (level <= 0) {
			return generatePollResultsKey("", value);
		} else {
			return generatePollResultsKey(String.valueOf(level), value);
		}
	}

	public static String generatePollResultsKey(String level, String value) {
		String key = level;
		if (TextUtils.isEmpty(key)) {
			key = value;
			int index = key.indexOf(" ");
			if (index > -1) {
				key = key.substring(0, index);
			}
		}
		return key;
	}
}
