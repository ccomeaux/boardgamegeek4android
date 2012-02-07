package com.boardgamegeek.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.text.format.DateUtils;

public class DateTimeUtils {

	private static final String BGG_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssz";
	private static final DateFormat FORMATER = new SimpleDateFormat(BGG_DATE_FORMAT, Locale.US);

	private DateTimeUtils() {
	}

	public static int howManyDaysOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.DAY_IN_MILLIS);
	}

	public static int howManyHoursOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.HOUR_IN_MILLIS);
	}

	public static int howManyMinutesOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.MINUTE_IN_MILLIS);
	}

	public static long parseDate(String inDate) {
		inDate = fixupTimeZone(inDate);
		try {
			final Date date = FORMATER.parse(inDate);
			return date.getTime();
		} catch (ParseException e) {
			return 0;
		}

	}

	private static String fixupTimeZone(String inDate) {
		int index = inDate.lastIndexOf("-");

		if (index > 0) {
			inDate = inDate.substring(0, index).concat("GMT").concat(inDate.substring(index));
		}
		return inDate;
	}
}
