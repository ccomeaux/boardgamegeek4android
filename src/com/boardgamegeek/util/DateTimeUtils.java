package com.boardgamegeek.util;

import android.text.format.DateUtils;

public class DateTimeUtils {
	
	public static int howManyDaysOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.DAY_IN_MILLIS);
	}

	private DateTimeUtils(){}
}
